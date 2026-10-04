package com.labs_101.backend.repositories;

/**
 * Result of a fuzzy name search, see {@link FoodMatchProjection#SCORE}.
 */
public interface FoodMatchProjection {
    /**
     * The main part of a name: lowercase, without parentheses and everything
     * after the first comma, e.g. "Cappuccino (Getränk) mit Milch 3,5 % Fett"
     * -> "cappuccino mit milch 3".
     */
    String HEAD = "trim(regexp_replace(regexp_replace(lower(name), '\\([^)]*\\)', ' ', 'g'), ',.*$', ''))";

    /**
     * Ranks how well {@code name} matches {@code :query} (lowercase).
     * <ul>
     * <li>trigram similarity, whole word matches count most</li>
     * <li>bonus if the head equals the query, also when it is written apart
     * ("Hafer Flocken" for "haferflocken")</li>
     * <li>bonus if the head starts with the query ("Banane roh" for "banane",
     * "Hähnchen Brust, roh" for "hähnchenbrust")</li>
     * <li>raw foods are preferred, processed forms (Mehl, Pulver, ...) only if
     * asked for</li>
     * <li>every word of the head costs a little, so plain foods win over
     * dishes</li>
     * </ul>
     */
    String SCORE = "( 0.5 * strict_word_similarity(:query, lower(name))"
            + " + 0.3 * similarity(lower(name), :query)"
            + " + 0.2 * word_similarity(:query, lower(name))"
            + " + CASE"
            + "     WHEN replace(" + HEAD + ", ' ', '') = :query THEN 0.5"
            + "     WHEN split_part(" + HEAD + ", ' ', 1) = :query"
            + "       OR split_part(" + HEAD + ", ' ', 1) || split_part(" + HEAD + ", ' ', 2) = :query THEN 0.2"
            + "     ELSE 0 END"
            + " + CASE WHEN lower(name) ~ '\\mroh$' THEN 0.05 ELSE 0 END"
            + " - CASE WHEN lower(name) ~ '\\m(mehl|stärke|pulver|instantpulver|kleie|grieß|öl|sirup|extrakt|konzentrat)\\M'"
            + "     AND :query !~ '(mehl|stärke|pulver|kleie|grieß|öl|sirup|extrakt|konzentrat)' THEN 0.15 ELSE 0 END"
            + " - 0.02 * array_length(regexp_split_to_array(" + HEAD + ", '\\s+'), 1) )";

    // uses the trigram index on lower(name)
    String CANDIDATES = "(lower(name) % :query OR :query <% lower(name))";

    Long getId();

    String getName();

    Double getScore();
}
