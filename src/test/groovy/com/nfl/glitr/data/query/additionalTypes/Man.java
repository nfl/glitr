package com.nfl.glitr.data.query.additionalTypes;

public class Man implements Person {
        public String getId() {
            return id;
        }

        public String getFirstName() {
            return firstName;
        }

        @Override
        public String getAge() {
            return this.age;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public void setAge(String age) {
            this.age = age;
        }

        private String id;
        private String firstName;
        private String age;
    }