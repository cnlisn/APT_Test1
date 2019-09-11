package com.lisn.apt_test1;

import com.lisn.annotationlib.Route;
import com.lisn.annotationlib.Test;

@Test("this is class TestClass")
public class TestClass<T> implements TestInterface{
    @Test("this is local field name")
    private String name = "my name is test";

    @Test("this is local method sayHello")
    private String sayHello(@Test("this is parameter msg") String msg){
        String hello = "my name is hello";
        return hello;
    }
}