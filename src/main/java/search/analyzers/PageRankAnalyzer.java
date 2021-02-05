package search.analyzers;

import datastructures.concrete.ChainedHashSet;
import datastructures.concrete.KVPair;
import datastructures.concrete.dictionaries.ChainedHashDictionary;
import datastructures.interfaces.IDictionary;
import datastructures.interfaces.ISet;
import search.models.Webpage;

import java.net.URI;

/**
 * This class is responsible for computing the 'page rank' of all available webpages.
 * If a webpage has many different links to it, it should have a higher page rank.
 * See the spec for more details.
 */
public class PageRankAnalyzer {
    private IDictionary<URI, Double> pageRanks;

    /**
     * Computes a graph representing the internet and computes the page rank of all
     * available webpages.
     *
     * @param webpages  A set of all webpages we have parsed.
     * @param decay     Represents the "decay" factor when computing page rank (see spec).
     * @param epsilon   When the difference in page ranks is less then or equal to this number,
     *                  stop iterating.
     * @param limit     The maximum number of iterations we spend computing page rank. This value
     *                  is meant as a safety valve to prevent us from infinite looping in case our
     *                  page rank never converges.
     */
    public PageRankAnalyzer(ISet<Webpage> webpages, double decay, double epsilon, int limit) {
        // Implementation note: We have commented these method calls out so your
        // search engine doesn't immediately crash when you try running it for the
        // first time.
        //
        // You should uncomment these lines when you're ready to begin working
        // on this class.

        // Step 1: Make a graph representing the 'internet'
        IDictionary<URI, ISet<URI>> graph = this.makeGraph(webpages);

        // Step 2: Use this graph to compute the page rank for each webpage
        this.pageRanks = this.makePageRanks(graph, decay, limit, epsilon);

        // Note: we don't store the graph as a field: once we've computed the
        // page ranks, we no longer need it!
    }

    /**
     * This method converts a set of webpages into an unweighted, directed graph,
     * in adjacency list form.
     *
     * You may assume that each webpage can be uniquely identified by its URI.
     *
     * Note that a webpage may contain links to other webpages that are *not*
     * included within set of webpages you were given. You should omit these
     * links from your graph: we want the final graph we build to be
     * entirely "self-contained".
     */
    public IDictionary<URI, ISet<URI>> makeGraph(ISet<Webpage> webpages) {
        IDictionary<URI, ISet<URI>> graph = new ChainedHashDictionary<>();
        //For each webpage, create a set of other pages that this page links to
        for (Webpage page : webpages) {
            ISet<URI> adj = new ChainedHashSet<>();
            for (URI link : page.getLinks()) {
                //Create a blank page w/ the URI of the current linked site to test if it exists in the webpages set
                Webpage newPage = new Webpage(link, null, null, null, null);
                if (!link.equals(page.getUri()) && webpages.contains(newPage)) {
                    adj.add(link);  //Add the link if it is in the set and is not linking to itself
                }
            }
            //Add this set of links as an adjancency list
            graph.put(page.getUri(), adj);
        }
        return graph;
    }

    /**
     * Computes the page ranks for all webpages in the graph.
     *
     * Precondition: assumes 'this.graphs' has previously been initialized.
     *
     * @param decay     Represents the "decay" factor when computing page rank (see spec).
     * @param epsilon   When the difference in page ranks is less then or equal to this number,
     *                  stop iterating.
     * @param limit     The maximum number of iterations we spend computing page rank. This value
     *                  is meant as a safety valve to prevent us from infinite looping in case our
     *                  page rank never converges.
     */
    private IDictionary<URI, Double> makePageRanks(IDictionary<URI, ISet<URI>> graph,
                                                   double decay,
                                                   int limit,
                                                   double epsilon) {

        //Step 1: Initialize the ranks as 1/N where N is number of vertices
        IDictionary<URI, Double> ranks = new ChainedHashDictionary<>();
        for (KVPair<URI, ISet<URI>> pair : graph) {
            ranks.put(pair.getKey(), 1.0 / graph.size());
        }

        //Step 2: Update the ranks w/ max iterations of limit, until the epsilon is met
        IDictionary<URI, Double> newRanks;
        for (int i = 0; i < limit; i++) {
            //Set newRanks to (1-d)/N (to represent random surfers joining the graph)
            newRanks = updateRanks(graph, ranks, decay);

            //Step 3: Check to see if ranks converge | oldrank - newrank | > epsilon
            if (convergenceCheckStep(ranks, newRanks, epsilon)) {
                return newRanks;
            }
            //Set old rank to this iterations result and iterate again
            ranks = newRanks;
        }
        return ranks;
    }

    /**
     * Returns the page rank of the given URI.
     *
     * Precondition: the given uri must have been one of the uris within the list of
     *               webpages given to the constructor.
     */
    public double computePageRank(URI pageUri) {
        return pageRanks.get(pageUri);
    }

    private IDictionary<URI, Double> updateRanks(IDictionary<URI, ISet<URI>> graph,
                                                 IDictionary<URI, Double> ranks, double decay) {
        IDictionary<URI, Double> newRanks = new ChainedHashDictionary<>();
        for (KVPair<URI, ISet<URI>> pair : graph) {
            newRanks.put(pair.getKey(), (1.0 - decay) / graph.size());
        }
        //For each page, spread surfers via links
        for (KVPair<URI, ISet<URI>> pair : graph) {
            if (pair.getValue().isEmpty()) {
                //If this page doesn't link to any other page, then add (d * oldRank)/N
                for (KVPair<URI, ISet<URI>> pair2 : graph) {
                    double oldRank = ranks.get(pair.getKey());
                    double newRank = (decay * oldRank) / graph.size();
                    newRank += newRanks.get(pair2.getKey());
                    newRanks.put(pair2.getKey(), newRank);
                }
            } else {
                //Otherwise, spread surfers to linked pages
                for (URI link : pair.getValue()) {
                    double oldRank = ranks.get(pair.getKey());
                    double newRank = (decay * oldRank) / pair.getValue().size();
                    newRank += newRanks.get(link);
                    newRanks.put(link, newRank);
                }
            }
        }
        return newRanks;
    }

    /**
     * Helper method that checks if the ranks converge
     * @param oldRank - the dictionary holding the old rank for each page (URI)
     * @param newRank - the dictionary holding the new rank for each page (URI)
     * @param epsilon - the threshold to compare against
     * @return - true if and only if the difference between the new and old rank of every result is less than epsilon
     *          -false, otherwise
     */
    private boolean convergenceCheckStep(IDictionary<URI, Double> oldRank, IDictionary<URI, Double> newRank,
                                         double epsilon) {
        for (KVPair<URI, Double> pair : oldRank) {
            double diff = pair.getValue() - newRank.get(pair.getKey());
            if (Math.abs(diff) >= epsilon) {
                return false;
            }
        }
        return true;
    }
}
