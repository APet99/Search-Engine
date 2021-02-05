package search.analyzers;


import datastructures.concrete.ChainedHashSet;
import datastructures.concrete.KVPair;
import datastructures.concrete.dictionaries.ChainedHashDictionary;
import datastructures.interfaces.IDictionary;
import datastructures.interfaces.IList;
import datastructures.interfaces.ISet;
import search.models.Webpage;

import java.net.URI;

/**
 * This class is responsible for computing how "relevant" any given document is
 * to a given search query.
 *
 * See the spec for more details.
 */
public class TfIdfAnalyzer {
    // This field must contain the IDF score for every single word in all
    // the documents.
    private IDictionary<String, Double> idfScores;

    // This field must contain the TF-IDF vector for each webpage you were given
    // in the constructor.
    //
    // We will use each webpage's page URI as a unique key.
    private IDictionary<URI, IDictionary<String, Double>> documentTfIdfVectors;

    // Feel free to add extra fields and helper methods.

    public TfIdfAnalyzer(ISet<Webpage> webpages) {
        this.idfScores = this.computeIdfScores(webpages);
        this.documentTfIdfVectors = this.computeAllDocumentTfIdfVectors(webpages);
    }

    // Note: this method, strictly speaking, doesn't need to exist. However,
    // we've included it so we can add some unit tests to help verify that your
    // constructor correctly initializes your fields.
    public IDictionary<URI, IDictionary<String, Double>> getDocumentTfIdfVectors() {
        return this.documentTfIdfVectors;
    }

    // Note: these private methods are suggestions or hints on how to structure your
    // code. However, since they're private, you're not obligated to implement exactly
    // these methods: feel free to change or modify these methods however you want. The
    // important thing is that your 'computeRelevance' method ultimately returns the
    // correct answer in an efficient manner.

    /**
     * Return a dictionary mapping every single unique word found
     * in every single document to their IDF score.
     */
    private IDictionary<String, Double> computeIdfScores(ISet<Webpage> pages) {
        //Build dictionary of unique words as the key and set of pages containing that word as the value
        IDictionary<String, ISet<Webpage>> wordsOfPage = new ChainedHashDictionary<>();
        for (Webpage page : pages) {
            for (String word : page.getWords()) {
                if (wordsOfPage.containsKey(word)) {
                    wordsOfPage.get(word).add(page);
                } else {
                    //Uses smaller size, since we are instantiating lots of sets, can always resize
                    ISet<Webpage> set = new ChainedHashSet<>(151);
                    set.add(page);
                    wordsOfPage.put(word, set);
                }

            }
        }
        //Iterate through each word, calculating the idf score for each
        IDictionary<String, Double> scores = new ChainedHashDictionary<>();
        for (KVPair<String, ISet<Webpage>> pair : wordsOfPage) {
            double score = Math.log((pages.size() * 1.0) / (pair.getValue().size() * 1.0));
            scores.put(pair.getKey(), score);
        }
        return scores;
    }

    /**
     * Returns a dictionary mapping every unique word found in the given list
     * to their term frequency (TF) score.
     *
     * The input list represents the words contained within a single document.
     */
    private IDictionary<String, Double> computeTfScores(IList<String> words) {
        IDictionary<String, Double> scores = new ChainedHashDictionary<>();
        for (String word : words) {
            if (scores.containsKey(word)) {
                scores.put(word, scores.get(word) + (1.0 / words.size()));
            } else {
                scores.put(word, 1.0 / words.size());
            }
        }
        return scores;
    }

    /**
     * See spec for more details on what this method should do.
     */
    private IDictionary<URI, IDictionary<String, Double>> computeAllDocumentTfIdfVectors(ISet<Webpage> pages) {
        // Hint: this method should use the idfScores field and
        // call the computeTfScores(...) method.
        IDictionary<URI, IDictionary<String, Double>> vector = new ChainedHashDictionary<>();
        for (Webpage page : pages) {
            vector.put(page.getUri(), computeTfIdfScore(page.getWords()));
        }
        return vector;
    }

    /**
     * Returns the cosine similarity between the TF-IDF vector for the given query and the
     * URI's document.
     *
     * Precondition: the given uri must have been one of the uris within the list of
     *               webpages given to the constructor.
     */
    public Double computeRelevance(IList<String> query, URI pageUri) {
        IDictionary<String, Double> documentVector = documentTfIdfVectors.get(pageUri);
        IDictionary<String, Double> queryVector = computeTfIdfScore(query);

        double numerator = 0.0;
        for (String word : query) {
            double docWordScore = (documentVector.containsKey(word)) ? documentVector.get(word) : 0.0;
            double queryWordScore = queryVector.get(word);
            numerator += docWordScore * queryWordScore;
        }
        double denominator = norm(documentVector) * norm(queryVector);

        return (denominator == 0) ? 0.0 : numerator / denominator;
    }

    /**
     * Helper method that is used to help compute the TfIdf score
     * @param words - The list of words to compute the score for
     * @return - An IDictionary w/ each word from the input list as a key
     *              and the TfIdf score as the value
     */
    private IDictionary<String, Double> computeTfIdfScore(IList<String> words) {
        IDictionary<String, Double> tfScore = computeTfScores(words);
        IDictionary<String, Double> tfidfScore = new ChainedHashDictionary<>();
        for (KVPair<String, Double> pair : tfScore) {
            if (idfScores.containsKey(pair.getKey())) {
                tfidfScore.put(pair.getKey(), pair.getValue() * idfScores.get(pair.getKey()));
            } else {
                tfidfScore.put(pair.getKey(), 0.0);
            }
        }
        return tfidfScore;
    }

    /**
     * Helper method used in computing the relevance of a query
     * @param vector - The map of words/scores to compute
     * @return - the normalized score
     */
    private double norm(IDictionary<String, Double> vector) {
        double output = 0.0;
        for (KVPair<String, Double> pair : vector) {
            double score = pair.getValue();
            output += score * score;
        }
        return Math.sqrt(output);
    }
}
