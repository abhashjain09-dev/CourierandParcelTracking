package com.courier.util;

import java.util.Scanner;

public class ConsoleHelper {
    public static final String RESET  = "\u001B[0m";
    public static final String RED    = "\u001B[31m";
    public static final String GREEN  = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE   = "\u001B[34m";
    public static final String CYAN   = "\u001B[36m";
    public static final String WHITE  = "\u001B[37m";
    public static final String BOLD   = "\u001B[1m";
    public static final String PURPLE = "\u001B[35m";

    private static final Scanner scanner = new Scanner(System.in);

    private ConsoleHelper() {}

    public static String readLine(String prompt) {
        System.out.print(CYAN + prompt + RESET);
        return scanner.nextLine().trim();
    }

    public static String readLineOptional(String prompt) {
        System.out.print(CYAN + prompt + " (Enter to skip): " + RESET);
        return scanner.nextLine().trim();
    }

    public static int readInt(String prompt) {
        while (true) {
            try {
                System.out.print(CYAN + prompt + RESET);
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                printError("Please enter a valid integer.");
            }
        }
    }

    public static double readDouble(String prompt) {
        while (true) {
            try {
                System.out.print(CYAN + prompt + RESET);
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                printError("Please enter a valid number.");
            }
        }
    }

    public static boolean readBoolean(String prompt) {
        while (true) {
            System.out.print(CYAN + prompt + " (y/n): " + RESET);
            String in = scanner.nextLine().trim().toLowerCase();
            if (in.equals("y") || in.equals("yes")) return true;
            if (in.equals("n") || in.equals("no"))  return false;
            printError("Please enter y or n.");
        }
    }

    public static void printSuccess(String msg) { System.out.println(GREEN  + "[SUCCESS] " + msg + RESET); }
    public static void printError(String msg)   { System.out.println(RED    + "[ERROR]   " + msg + RESET); }
    public static void printInfo(String msg)    { System.out.println(BLUE   + "[INFO]    " + msg + RESET); }
    public static void printWarning(String msg) { System.out.println(YELLOW + "[WARNING] " + msg + RESET); }

    public static void printSeparator() {
        System.out.println(YELLOW + "=".repeat(70) + RESET);
    }

    public static void printHeader(String title) {
        int w = 68;
        int pad = (w - title.length()) / 2;
        System.out.println();
        System.out.println(PURPLE + "+" + "=".repeat(w) + "+" + RESET);
        System.out.println(PURPLE + "|" + BOLD + " ".repeat(pad) + title
            + " ".repeat(w - pad - title.length()) + RESET + PURPLE + "|" + RESET);
        System.out.println(PURPLE + "+" + "=".repeat(w) + "+" + RESET);
    }

    public static void printSubHeader(String title) {
        System.out.println(BOLD + BLUE + "\n-- " + title + " " + "-".repeat(Math.max(0, 58 - title.length())) + RESET);
    }

    public static void printRow(String label, String value) {
        System.out.printf("  %-28s : %s%n", BOLD + label + RESET, value);
    }

    public static void pressEnterToContinue() {
        System.out.print(YELLOW + "\n  Press ENTER to continue..." + RESET);
        scanner.nextLine();
    }
}
