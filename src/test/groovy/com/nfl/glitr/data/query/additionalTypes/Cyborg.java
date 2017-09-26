package com.nfl.glitr.data.query.additionalTypes;

public class Cyborg implements Person {

    private String id;
    private String codeName;
    private String age;


    public String getId() {
        return id;
    }

    public String getCodeName() {
        return codeName;
    }

    @Override
    public String getAge() {
        return this.age;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCodeName(String codeName) {
        this.codeName = codeName;
    }

    public void setAge(String age) {
        this.age = age;
    }
}