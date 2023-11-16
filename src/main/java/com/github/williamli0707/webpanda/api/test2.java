package com.github.williamli0707.webpanda.api;

import com.github.williamli0707.webpanda.records.Attempt;
import com.helger.commons.string.util.LevenshteinDistance;

import java.io.FileNotFoundException;
import java.util.*;

public class test2 {
    public static void main (String[] args) throws FileNotFoundException {
//        minTimeDiffBetweenProblems(new String[] {"lhs_6_4", "lhs_6_1", "lhs_6_5", "lhs_6_10", "lhs_6_13", "lhs_6_14",
//                "lhs_6_7", "lhs_6_2", "lhs_6_3", "lhs_6_8", "lhs_6_9"});
//        minTimeDiffBetweenProblems(new String[] {"lhs_6_8", "lhs_6_9"});
//        lastTimeDiffBetweenProblems(new String[] {"lhs_6_8", "lhs_6_9"});
//        case2();
//        RunestoneAPI api = new RunestoneAPI();
//        api.minTimeDiff(new String[] {"lhs_6_8", "lhs_6_9", "<a href=\\\"/runestone/dashboard/exercisemetrics?id=lhs_6_1&chapter=Functions\\\">lhs_6_1</a>"});
//        System.out.println(api.getAllCode("<a href=\\\"/runestone/dashboard/exercisemetrics?id=lhs_6_1&chapter=Functions\\\">lhs_6_1</a>"));
//        System.out.println(api.getAllCode("4. Python Turtle Graphics/4.1 Hello Little Turtles!"));

//        System.out.println();

//        System.out.println(api.requestHistory("lhs_5230007", "lhs_6_1"));
        /*
        can use a link like:
        https://runestone.academy/runestone/assignments/grades_report?report_type=assignment&chap_or_assign=Unit%206%20Problem%20Set
        to get all problems of a problem set
         */

//        findEdits();
        RunestoneAPI api = new RunestoneAPI();
        Callback callback = new GenericCallback();
        api.findLargeEdits(api.getAllCodeMultiple(callback, "lhs_6_8", "lhs_6_9"), 2, callback);
    }

    static class GenericCallback implements Callback {

        @Override
        public void call(int percent, String message) {

        }
    }

    public static void minTimeDiffBetweenProblems(String[] pids) {

    }

    public static void lastTimeDiffBetweenProblems(String[] pids) {
        RunestoneAPI api = new RunestoneAPI();
        Hashtable<String, String> names = api.getNames();
        HashMap<String, ArrayList<Long>> timestamps = new HashMap<>();
        for(String pid: pids) {
            HashMap<String, LinkedList<Attempt>> hm = api.getAllCode(pid);
            for(String sid: hm.keySet()) {
                if(!timestamps.containsKey(sid)) timestamps.put(sid, new ArrayList<>());
                LinkedList<Attempt> cur = hm.get(sid);
                if(cur.size() == 0) continue;
                timestamps.get(sid).add(cur.get(cur.size() - 1).timestamp()); //TODO verify that the last one is always most recent
            }
        }
        for(String i: names.keySet()) {
            double min = 1e9, avg = 0;
            if(timestamps.get(i).size() == 0) {
                System.out.println(i + " skipped - no data");
                continue;
            }
            Collections.sort(timestamps.get(i));
            long prev = timestamps.get(i).get(0);
            for(int j = 1;j < timestamps.get(i).size();j++) {
                double curr = (timestamps.get(i).get(j) - prev) / 1000d;
                min = Math.min(min, curr);
                avg += curr;
                prev = timestamps.get(i).get(j);
            }
            avg /= timestamps.get(i).size() - 1;
            System.out.println(names.get(i) + " min: " + min + " avg: " + avg);
        }
    }

    // just print all code
    public static void printAll(String pid) {
        RunestoneAPI api = new RunestoneAPI();
        Hashtable<String, String> names = api.getNames();
        HashMap<String, LinkedList<Attempt>> code = api.getAllCode(pid);
        System.out.println("\n\n\n\n\n\n\n\n\n");
        for(String k: code.keySet()) {
            LinkedList<Attempt> i = code.get(k);
            int ind = 0;
            for(Attempt j: i) {
                ind++;
                System.out.println(k + " " + names.get(k) + " attempt " + ind);
                System.out.println(j.code());
            }
        }
    }

    //Find all edits over 500 - specific to lhs_6_9 as it is a long problem?
    public static void findEdits() {
        RunestoneAPI api = new RunestoneAPI();
        HashMap<String, ArrayList<Double>> h = api.getMultipleProblemsND(new String[] {"lhs_6_9"});
        ArrayList<Double> diffs = new ArrayList<>();
        for(String k: h.keySet()) {
            ArrayList<Double> i = h.get(k);
            diffs.addAll(i);
            for(double j: i) {
                if(j >= 500) System.out.println(k + " " + j);
            }
        }
        diffs.sort(Double::compareTo);
        for (double cur : diffs) {
            System.out.printf("%.1f, ", cur);
        }
    }

    //test for specific student
    public static void case2() {
        String[] codes = new String[] {
                "# My Name: Lehem Atsebha\n\ndef tree(n):\n    # write your code here\n\ndef trunk(n):\n    # write your code here\n\ndef drawTree(base):\n    tree(base)\n    trunk(base)\n\ndef main():\n    h = int(input(\"Enter the height of the tree:\"))\n    drawTree( h )\n\nif __name__ == \"__main__\":\n    main()\n\n",
                "# My Name: Lehem Atsebha\n\ndef tree(n):\n    # write your code here\n    for i in range(n):\n        print(\" \"*(n-i-1), end=\"\")\n        print(\"*\"*(i*2)+\"*\")\n        \ndef trunk(n):\n    # write your code here\n    for i in range(3):\n        print(\" \"*(n-3),\"*\"*3)\n        \ndef drawTree(base):\n    tree(base)\n    trunk(base)\n\ndef main():\n    h = int(input(\"Enter the height of the tree:\"))\n    drawTree( h )\n\nif __name__ == \"__main__\":\n    main()\n\n",
                "# My Name: Lehem Atsebha\n\ndef tree(n):\n    # write your code here\n    for i in range(n):\n        print(\" \"*(n-i-1), end=\"\")\n        print(\"*\"*(i*2)+\"*\")\n        \n        # \n        \ndef trunk(n):\n    # write your code here\n    for i in range(3):\n        print(\" \"*(n-3),\"*\"*3)\n        \ndef drawTree(base):\n    tree(base)\n    trunk(base)\n\ndef main():\n    h = int(input(\"Enter the height of the tree:\"))\n    drawTree( h )\n\nif __name__ == \"__main__\":\n    main()\n\n"
        },
            codes2 = new String[] {
                "# My Name: Lehem Atsebha\n\ndef wedgeOfStars( n ):\n    # write your code here\n\ndef main():\n    h = int(input(\"Initial number of stars:\"))\n    wedgeOfStars( h )\n\nif __name__ == \"__main__\":\n    main()\n\n",
                "# My Name: Lehem Atsebha\n\ndef wedgeOfStars( n ):\n    # write your code here\n    for i in range (n, 0, -1):\n        print(\"*\"*(i))\n        \ndef main():\n    h = int(input(\"Initial number of stars:\"))\n    wedgeOfStars( h )\n\nif __name__ == \"__main__\":\n    main()\n\n"
            };
        String[] timestamps = new String[] {
                "2023-10-12T22:14:47.421323+00:00",
                "2023-10-15T06:54:03.468127+00:00",
                "2023-10-26T19:56:34.302788+00:00"
        },
            ts2 = new String[] {
                "2023-10-12T22:14:45.613713+00:00",
                "2023-10-15T06:55:01.279823+00:00"

            };
//        Long[] times = Arrays.stream(timestamps).map(s->DateParserUtils.parseDate(s).getTime()).toList().toArray(new Long[0]),
//            times2 = Arrays.stream(ts2).map(s->DateParserUtils.parseDate(s).getTime()).toList().toArray(new Long[0]);
//
//        System.out.println("lhs_6_9");
//        for(int i = 1;i < codes.length;i++) System.out.printf("%.3f ", (LevenshteinDistance.getDistance(codes[i - 1], codes[i])) * 1d);
//        System.out.println();
//
//        System.out.println("lhs_6_8");
//        for(int i = 1;i < codes2.length;i++) System.out.printf("%.3f ", (LevenshteinDistance.getDistance(codes2[i - 1], codes2[i])) * 1d);
//        System.out.println();
        System.out.println((LevenshteinDistance.getDistance(codes2[0], codes2[1]) * 1.0f) / codes2[0].length());
    }

    //test for specific students
    public static void case1() {
        System.out.println("Akhil");
        String[] codes = new String[] {
                "def drawTree(height):\n    for i in range(1, height + 1):\n        spaces = \" \" * (height - i)\n        stars = \"*\" * (2 * i - 1)\n        print(spaces + stars, end='')\n        if i == 1:\n            print() \n        else:\n            print(stars, end='')  \n            print()  \n\ndef drawTreeBase():\n    for _ in range(3):\n        spaces = \" \" * (height - 1)\n        trunk = \"*\" * 3 \n        print(spaces + trunk)\nheight = int(input(\"Enter the height of the tree: \"))\ndrawTree(height)\ndrawTreeBase()\n\n",
                "def Tree(height):\n    for i in range(1, height + 1):\n        spaces = \" \" * (height - i)\n        stars = \"*\" * (2 * i - 1)\n        print(spaces + stars, end='')\n        if i == 1:\n            print() \n        else:\n            print(stars, end='')  \n            print()  \n\ndef drawTreeBase():\n    for _ in range(3):\n        spaces = \" \" * (height - 1)\n        trunk = \"*\" * 3 \n        print(spaces + trunk)\nheight = int(input(\"Enter the height of the tree: \"))\nTree(height)\ndrawTreeBase()\n\n",
                "def tree(height):\n    for i in range(1, height + 1):\n        spaces = \" \" * (height - i)\n        stars = \"*\" * (2 * i - 1)\n        print(spaces + stars, end='')\n        if i == 1:\n            print() \n        else:\n            print(stars, end='')  \n            print()  \n\ndef drawTreeBase():\n    for _ in range(3):\n        spaces = \" \" * (height - 1)\n        trunk = \"*\" * 3 \n        print(spaces + trunk)\nheight = int(input(\"Enter the height of the tree: \"))\ntree(height)\ndrawTreeBase()\n\n",
                "def tree(height):\n    for i in range(1, height + 1):\n        spaces = \" \" * (height - i)\n        stars = \"*\" * (2 * i - 1)\n        print(spaces + stars, end='')\n        if i == 1:\n            print() \n        else:\n            print(stars, end='')  \n            print()  \n\ndef trunk():\n    for _ in range(3):\n        spaces = \" \" * (height - 1)\n        trunk = \"*\" * 3 \n        print(spaces + trunk)\nheight = int(input(\"Enter the height of the tree: \"))\ntree(height)\ntrunk()\n\n",
                "def tree(height):\n    for i in range(1, height):\n        spaces = \" \" * (height - i)\n        stars = \"*\" * (2 * i - 1)\n        print(spaces + stars, end='')\n        if i == 1:\n            print() \n        else:\n            print(stars, end='')  \n            print()  \n\ndef trunk():\n    for _ in range(3):\n        spaces = \" \" * (height - 1)\n        trunk = \"*\" * 3 \n        print(spaces + trunk)\nheight = int(input(\"Enter the height of the tree: \"))\ntree(height)\ntrunk()\n\n",
                "def tree(height):\n    for i in range(1, height):\n        spaces = \" \" * (height - i)\n        stars = \"*\" * (2 * i - 1)\n        print(spaces + stars, end='')\n        if i == 1:\n            print() \n        else:\n            print(stars, end='')  \n            print()  \n\ndef trunk():\n    for i in range(3):\n        spaces = \" \" * (height)\n        trunk = \"*\" * 3 \n        print(spaces + trunk)\nheight = int(input(\"Enter the height of the tree: \"))\ntree(height)\ntrunk()\n\n",
                "def tree(height):\n    for i in range(1, height):\n        spaces = \" \" * (height - i)\n        stars = \"*\" * (2 * i - 1)\n        print(spaces + stars, end='')\n        if i == 1:\n            print() \n        else:\n            print(stars, end='')  \n            print()  \n\ndef trunk(height):\n    for i in range(3):\n        spaces = \" \" * (height)\n        trunk = \"*\" * 3 \n        print(spaces + trunk)\nheight = int(input(\"Enter the height of the tree: \"))\ntree(height)\ntrunk()\n\n",
                "def tree(height):\n    for i in range(1, height):\n        spaces = \" \" * (height - i)\n        stars = \"*\" * (2 * i - 1)\n        print(spaces + stars, end='')\n        if i == 1:\n            print() \n        else:\n            print(stars, end='')  \n            print()  \n\ndef trunk(height):\n    for i in range(3):\n        spaces = \" \" * (height)\n        trunk = \"*\" * 3 \n        print(spaces + trunk)\nheight = int(input(\"Enter the height of the tree: \"))\ntree(height)\ntrunk(height)\n\n",
                "def tree(height):\n    for i in range(1, height):\n        spaces = \" \" * (height - i)\n        stars = \"*\" * (2 * i)\n        print(spaces + stars, end='')\n        if i == 1:\n            print() \n        else:\n            print(stars, end='')  \n            print()  \n\ndef trunk(height):\n    for i in range(3):\n        spaces = \" \" * (height)\n        trunk = \"*\" * 3 \n        print(spaces + trunk)\nheight = int(input(\"Enter the height of the tree: \"))\ntree(height)\ntrunk(height)\n\n",
                "def tree(height):\n    for i in range(1, height):\n        spaces = \" \" * (height - i)\n        stars = \"*\" * (2 * i)\n        print(spaces + stars, end='')\n        if i == 1:\n            print() \n        else:\n            print(stars, end='')  \n            print()  \n\ndef trunk(height):\n    for i in range(3):\n        spaces = \"  \" * (height)\n        trunk = \"*\" * 3 \n        print(spaces + trunk)\nheight = int(input(\"Enter the height of the tree: \"))\ntree(height)\ntrunk(height)\n\n",
                "def tree(height):\n    for i in range(1, height):\n        spaces = \" \" * (height - i)\n        stars = \"*\" * (2 * i)\n        print(spaces + stars, end='')\n        if i == 1:\n            print() \n        else:\n            print(stars, end='')  \n            print()  \n\ndef trunk(height):\n    for i in range(3):\n        spaces = \"\" * (height)\n        trunk = \"*\" * 3 \n        print(spaces + trunk)\nheight = int(input(\"Enter the height of the tree: \"))\ntree(height)\ntrunk(height)\n\n",
                "def tree(height):\n    for i in range(1, height):\n        spaces = \" \" * (height - i)\n        stars = \"*\" * (2 * i)\n        print(spaces + stars, end='')\n        if i == 1:\n            print() \n        else:\n            print(stars, end='')  \n            print()  \n\ndef trunk(height):\n    for i in range(3):\n        spaces = \" \" * (height)\n        trunk = \"*\" * 3 \n        print(spaces + trunk)\nheight = int(input(\"Enter the height of the tree: \"))\ntree(height)\ntrunk(height)\n\n",
                "def tree(height):\n    for i in range(1, height):\n        spaces = \" \" * (height - i)\n        stars = \"*\" * (2 * i)\n        print(spaces + stars, end='')\n        if i == 1:\n            print() \n        else:\n            print(stars, end='')  \n            print()  \n\ndef trunk(height):\n    for i in range(3):\n        spaces = \" \" * (height/2)\n        trunk = \"*\" * 3 \n        print(spaces + trunk)\nheight = int(input(\"Enter the height of the tree: \"))\ntree(height)\ntrunk(height)\n\n",
                "# My name is: Akhil \ndef tree(n):\n    iterations = 1\n    spaces = n-1\n    for k in range(0,n):\n        for x in range(0, spaces):\n            print(\" \", end='')\n        spaces = spaces - 1\n        for x in range(iterations):\n            print(\"*\", end='')\n        iterations = iterations + 2\n        print()\n    # write your code here\n   \ndef trunk(n):\n    # write your code here\n    for i in [1,2,3]:\n       \n        for x in range(0, (n-2)):                #Loop to print the spaces\n            print(\" \", end='')\n\n        for x in range(3):                         #Loop to print the stars\n            print(\"*\", end='')\n        print()\n   \ndef drawTree(base):\n    tree(base)\n    trunk(base)\n\ndef main():\n    h = int(input(\"Enter the height of the tree:\"))\n    drawTree( h )\n\nif __name__ == \"__main__\":\n    main()\n\n",
                "# My name is: Akhil \ndef tree(n):\n    iterations = 1\n    spaces = n-1\n    for k in range(0,n):\n        for x in range(0, spaces):\n            print(\" \", end='')\n        spaces = spaces - 1\n        for x in range(iterations):\n            print(\"*\", end='')\n        iterations = iterations + 2\n        print() \ndef trunk(n):\n    for i in [1,2,3]:      \n        for x in range(0, (n-2)):          \n            print(\" \", end='')\n        for x in range(3):                         \n            print(\"*\", end='')\n        print()   \ndef drawTree(base):\n    tree(base)\n    trunk(base)\ndef main():\n    h = int(input(\"Enter the height of the tree:\"))\n    drawTree( h )\nif __name__ == \"__main__\":\n    main()\n\n",
                "# My name is: Akhil \ndef tree(n):\n    iterations = 1\n    spaces = n-1\n    for k in range(0,n):\n        for x in range(0, spaces):\n            print(\" \", end='')\n        spaces = spaces - 1\n        for x in range(iterations):\n            print(\"*\", end='')\n        iterations = iterations + 2\n        print() \ndef trunk(n):\n    for i in [1,2,3]:      \n        for x in range(0, n-2):          \n            print(\" \", end='')\n        for x in range(3):                         \n            print(\"*\", end='')\n        print()   \ndef drawTree(base):\n    tree(base)\n    trunk(base)\ndef main():\n    h = int(input(\"Enter the height of the tree:\"))\n    drawTree( h )\nif __name__ == \"__main__\":\n    main()\n\n"
        };

        for(int i = 1;i < codes.length;i++) System.out.print(LevenshteinDistance.getDistance(codes[i - 1], codes[i]) + " ");
        System.out.println();
        for(int i = 1;i < codes.length;i++) System.out.printf("%.3f ", (LevenshteinDistance.getDistance(codes[i - 1], codes[i])) * 1d / codes[i - 1].length());
        System.out.println();

        System.out.println("Sreyes");
        codes = new String[] {
                "# My Name: Sreyes Chetput\n\ndef tree(n):\n    # write your code here\n    \ndef trunk(n):\n    # write your code here\n     for i in [1,2,3]:\n        for x in range(3):\n            print(\"*\", end='')\n        print()\n    \ndef drawTree(base):\n    tree(base)\n    trunk(base)\n\ndef main():\n    h = int(input(\"Enter the height of the tree:\"))\n    drawTree( h )\n\nif __name__ == \"__main__\":\n    main()\n\n",
                "# My Name: Sreyes Chetput\n\n#def tree(n):\n    # write your code here\n    \ndef trunk(n):\n    # write your code here\n     for i in [1,2,3]:\n        for x in range(3):\n            print(\"*\", end='')\n        print()\n    \ndef drawTree(base):\n    #tree(base)\n    trunk(base)\n\ndef main():\n    h = int(input(\"Enter the height of the tree:\"))\n    drawTree( h )\n\nif __name__ == \"__main__\":\n    main()\n\n",
                "# My Name: Sreyes Chetput\n\n#def tree(n):\n    # write your code here\n    \ndef trunk(n):\n    # write your code here\n    for i in [1,2,3]:\n        \n        for x in range(0, (n-2)+1):                #Loop to print the spaces\n            print(\" \", end='')\n            print()\n\n        for x in range(3):                         #Loop to print the stars\n            print(\"*\", end='')\n        print()\n    \ndef drawTree(base):\n    #tree(base)\n    trunk(base)\n\ndef main():\n    h = int(input(\"Enter the height of the tree:\"))\n    drawTree( h )\n\nif __name__ == \"__main__\":\n    main()\n\n",
                "# My Name: Sreyes Chetput\n\n#def tree(n):\n    # write your code here\n    \ndef trunk(n):\n    # write your code here\n    for i in [1,2,3]:\n        \n        for x in range(0, (n-2)+1):                #Loop to print the spaces\n            print(\" \", end='')\n\n        for x in range(3):                         #Loop to print the stars\n            print(\"*\", end='')\n        print()\n    \ndef drawTree(base):\n    #tree(base)\n    trunk(base)\n\ndef main():\n    h = int(input(\"Enter the height of the tree:\"))\n    drawTree( h )\n\nif __name__ == \"__main__\":\n    main()\n\n",
                "# My Name: Sreyes Chetput\n\ndef tree(n):\n    iterations = 1\n    spaces = n\n    for k in range(0,n + 1):\n        for x in range(0, spaces):\n            print(\" \", end='')\n        spaces = spaces - 1\n        for x in range(iterations):\n            print(\"*\", end='')\n        iterations = iterations + 2\n        print()\n    # write your code here\n    \ndef trunk(n):\n    # write your code here\n    for i in [1,2,3]:\n        \n        for x in range(0, (n-2)+1):                #Loop to print the spaces\n            print(\" \", end='')\n\n        for x in range(3):                         #Loop to print the stars\n            print(\"*\", end='')\n        print()\n    \ndef drawTree(base):\n    tree(base)\n    trunk(base)\n\ndef main():\n    h = int(input(\"Enter the height of the tree:\"))\n    drawTree( h )\n\nif __name__ == \"__main__\":\n    main()\n\n",
                "# My Name: Sreyes Chetput\n\ndef tree(n):\n    iterations = 1\n    spaces = n\n    for k in range(0,n + 1):\n        for x in range(0, spaces):\n            print(\" \", end='')\n        spaces = spaces - 1\n        for x in range(iterations):\n            print(\"*\", end='')\n        iterations = iterations + 2\n        print()\n    # write your code here\n    \ndef trunk(n):\n    # write your code here\n    for i in [1,2,3]:\n        \n        for x in range(0, (n-2)):                #Loop to print the spaces\n            print(\" \", end='')\n\n        for x in range(3):                         #Loop to print the stars\n            print(\"*\", end='')\n        print()\n    \ndef drawTree(base):\n    tree(base)\n    trunk(base)\n\ndef main():\n    h = int(input(\"Enter the height of the tree:\"))\n    drawTree( h )\n\nif __name__ == \"__main__\":\n    main()\n\n",
                "# My Name: Sreyes Chetput\n\ndef tree(n):\n    iterations = 1\n    spaces = n\n    for k in range(0,n):\n        for x in range(0, spaces):\n            print(\" \", end='')\n        spaces = spaces - 1\n        for x in range(iterations):\n            print(\"*\", end='')\n        iterations = iterations + 2\n        print()\n    # write your code here\n    \ndef trunk(n):\n    # write your code here\n    for i in [1,2,3]:\n        \n        for x in range(0, (n-2)):                #Loop to print the spaces\n            print(\" \", end='')\n\n        for x in range(3):                         #Loop to print the stars\n            print(\"*\", end='')\n        print()\n    \ndef drawTree(base):\n    tree(base)\n    trunk(base)\n\ndef main():\n    h = int(input(\"Enter the height of the tree:\"))\n    drawTree( h )\n\nif __name__ == \"__main__\":\n    main()\n\n",
                "# My Name: Sreyes Chetput\n\ndef tree(n):\n    iterations = 1\n    spaces = n-1\n    for k in range(0,n):\n        for x in range(0, spaces):\n            print(\" \", end='')\n        spaces = spaces - 1\n        for x in range(iterations):\n            print(\"*\", end='')\n        iterations = iterations + 2\n        print()\n    # write your code here\n    \ndef trunk(n):\n    # write your code here\n    for i in [1,2,3]:\n        \n        for x in range(0, (n-2)):                #Loop to print the spaces\n            print(\" \", end='')\n\n        for x in range(3):                         #Loop to print the stars\n            print(\"*\", end='')\n        print()\n    \ndef drawTree(base):\n    tree(base)\n    trunk(base)\n\ndef main():\n    h = int(input(\"Enter the height of the tree:\"))\n    drawTree( h )\n\nif __name__ == \"__main__\":\n    main()\n\n"
        };
        for(int i = 1;i < codes.length;i++) System.out.print(LevenshteinDistance.getDistance(codes[i - 1], codes[i]) + " ");
        System.out.println();
        for(int i = 1;i < codes.length;i++) System.out.printf("%.3f ", (LevenshteinDistance.getDistance(codes[i - 1], codes[i])) * 1d / codes[i - 1].length());
        System.out.println();
    }
}
