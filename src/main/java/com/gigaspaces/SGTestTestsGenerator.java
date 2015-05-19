package com.gigaspaces;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Boris
 * @since 10.2.0
 * Generate TGRID tests metadata by scanning permutation files and extracting annotations from test classes.
 * The output is JSON formed.
 */
public class SGTestTestsGenerator {

    private JSONObject output = new JSONObject();
    private Set<Method> methods;

    public SGTestTestsGenerator(Set<Method> methods) {
        this.methods = methods;
    }

    public void scanMethods() {
        JSONArray testsArray = new JSONArray();
        output.put("type", "sgtest");
        for (Method testMethod : methods){
            String testClassName = testMethod.getDeclaringClass().getName();
            String testMethodName = testMethod.getName();
            JSONObject test = new JSONObject();
            test.put("name", testClassName + "#" + testMethodName);
            try {
                scanTestMethodAnnotations(testMethod, test);
            } catch (Exception e) {
                System.out.println("Failed to load method annotations "+ e);
            }
            testsArray.add(test);

        }
        output.put("tests", testsArray);
    }

    private void scanTestMethodAnnotations(Method m, JSONObject test) throws InvocationTargetException, IllegalAccessException {
        List<String> annotationsString = new ArrayList<String>();
        int i = 0;
        for (Annotation a : m.getDeclaredAnnotations()){
            annotationsString.add(a.toString());
        }
        if(i == 0)
            System.out.println(annotationsString);

        test.put("annotations", annotationsString);
    }


    private String annotationValueToString(Object value) {
        String toPut;
        if (value instanceof String[]){
            toPut = Arrays.toString((String[]) value);
        }
        else if (value instanceof String) {
            toPut = (String) value;
        }
        else {
            toPut = value.toString();
        }
        return toPut;
    }

    /**
     * Generate JSON formed permutations file
     * @param permutationFile the output JSON file to be created
     */
    public void writeTestsToFile(File permutationFile) throws IOException {
        FileWriter file = new FileWriter(permutationFile);
        try{
            file.write(output.toJSONString());
        }
        finally {
            file.flush();
            file.close();
        }
    }
}
