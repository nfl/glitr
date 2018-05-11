package com.nfl.glitr.calculator;

import graphql.language.Argument;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree-like result presentation of GraphQL query score calculation
 */
public class QueryComplexityNode {

    private String name;
    private double weight;
    private double totalWeight;
    private List<Argument> arguments = new ArrayList<>();
    private List<QueryComplexityNode> children = new ArrayList<>();
    private boolean ignore;
    private String formula;


    public QueryComplexityNode() {
    }

    public QueryComplexityNode(String name) {
        this.name = name;
    }

    public QueryComplexityNode(String name, boolean ignore) {
        this.name = name;
        this.ignore = ignore;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return node name
     */
    public String getName() {
        return name;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     *
     * @return a weight of this node
     */
    public double getWeight() {
        return weight;
    }

    public void setTotalWeight(double totalWeight) {
        this.totalWeight = totalWeight;
    }

    /**
     *
     * @return total weight, that includes current node weight and total weight of all first level children
     */
    public double getTotalWeight() {
        return totalWeight;
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public void setArguments(List<Argument> arguments) {
        this.arguments = arguments;
    }

    public void addArgument(Argument argument) {
        if (arguments == null) {
            arguments = new ArrayList<>();
        }
        arguments.add(argument);
    }

    public void setChildren(List<QueryComplexityNode> children) {
        this.children = children;
    }

    public void addChild(QueryComplexityNode val) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(val);
    }

    /**
     *
     * @return first level children
     */
    public List<QueryComplexityNode> getChildren() {
        return children;
    }

    public boolean isIgnore() {
        return ignore;
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    @Override
    public String toString() {
        return "QueryComplexityNode{" +
                "name='" + name + '\'' +
                ", weight=" + weight +
                ", totalWeight=" + totalWeight +
                '}';
    }
}
