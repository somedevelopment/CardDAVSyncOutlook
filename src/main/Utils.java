/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package main;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 *
 * @author omikron
 */
public class Utils {

    /**
    * Adds the specified path to the java library path
    * Source: http://stackoverflow.com/questions/15409223/adding-new-paths-for-native-libraries-at-runtime-in-java
    * @param pathToAdd the path to add
    */
    public static void addLibraryPath(String pathToAdd) {
        try {
            final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
            usrPathsField.setAccessible(true);

            //get array of paths
            final String[] paths = (String[])usrPathsField.get(null);

            //add the new path
            final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
            newPaths[newPaths.length-1] = pathToAdd;
            usrPathsField.set(null, newPaths);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException ex) {
            Status.printStatusToConsole("Cannot add library path");
        }
    }
}