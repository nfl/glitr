package com.nfl.glitr.calculator;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree-like result presentation of GraphQL query score calculation
 */
public class QueryComplexityNode {

    private String name;
    private boolean field;
    private double weight;
    private double totalWeight;
    private List<QueryComplexityNode> childes;


    private QueryComplexityNode() {
    }

    private QueryComplexityNode(Builder builder) {
        name = builder.name;
        field = builder.field;
        weight = builder.weight;
        childes = builder.childes;

        double childesScore = 0;
        if(childes != null) {
            childesScore = childes.stream().mapToDouble(QueryComplexityNode::getTotalWeight).sum();
        }
        totalWeight = weight + childesScore;
    }

    public static Builder newBuilder(String name, boolean field) {
        return new Builder(name, field);
    }

    /**
     *
     * @return node name
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return is this node a {@link graphql.language.Field} node
     */
    public boolean isField() {
        return field;
    }

    /**
     *
     * @return a weight of this node
     */
    public double getWeight() {
        return weight;
    }

    /**
     *
     * @return total weight, that includes current node weight and total weight of all first level childes
     */
    public double getTotalWeight() {
        return totalWeight;
    }

    /**
     *
     * @return first level childes
     */
    public List<QueryComplexityNode> getChildes() {
        return childes;
    }


    public static final class Builder {
        private String name;
        private boolean field;
        private double weight;
        private List<QueryComplexityNode> childes;

        private Builder(String name, boolean field) {
            this.name = name;
            this.field = field;
        }

        public Builder withWeight(double val) {
            weight = val;
            return this;
        }

        public Builder withChildes(List<QueryComplexityNode> val) {
            childes = val;
            return this;
        }

        public Builder addChild(QueryComplexityNode val) {
            if (childes == null) {
                childes = new ArrayList<>();
            }
            childes.add(val);
            return this;
        }

        public QueryComplexityNode build() {
            return new QueryComplexityNode(this);
        }
    }
}
