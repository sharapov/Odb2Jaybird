/*
{{IS_RIGHT
	This program is distributed under LGPL Version 3.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
 */
package odbpack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @Sharapov
 *
 */
public class CmdLineArgs {

    private final Map<String, String> allArgs = new HashMap<>();
    private final Map<String, String> longArgs = new HashMap<>();
    private final Map<String, String> shortArgs = new HashMap<>();
    private final ArrayList<String> mainArgs = new ArrayList<>();
    private final Map<String, String[]> expectedArgs = new HashMap<>();

    public CmdLineArgs(String[] args, String[][] expectArgs) {
        if (expectArgs != null) {
            for (String[] largs : expectArgs) {
                for (int i = 0; i < largs.length; i++) {
                    switch (i) {
                        case 0:
                            if (largs[0] != null && !largs[0].isEmpty()) {
                                expectedArgs.put(largs[0], new String[]{
                                    (largs.length > 1 && largs[1] != null && !largs[1].isEmpty() ? largs[1] : null),
                                    (largs.length > 2 ? largs[2] : null)});
                            }
                            break;
                        case 1:
                            if (largs[1] != null && !largs[1].isEmpty()) {
                                expectedArgs.put(largs[1], new String[]{
                                    (expectedArgs.containsKey(largs[0]) ? largs[0] : null),
                                    (largs.length > 2 ? largs[2] : null)});
                            }
                            break;
                    }
                }
            }
        }
        String[] parts;
        for (String arg : args) {
            if (arg.startsWith("--")) {
                parts = arg.substring(2).split("=");
                if (!parts[0].isEmpty()) {
                    allArgs.put(parts[0], (parts.length > 1 ? parts[1] : null));
                    longArgs.put(parts[0], (parts.length > 1 ? parts[1] : null));
                }
            } else if (arg.startsWith("-")) {
                parts = arg.substring(1).split("=");
                if (!parts[0].isEmpty()) {
                    allArgs.put(parts[0], (parts.length > 1 ? parts[1] : null));
                    shortArgs.put(parts[0], (parts.length > 1 ? parts[1] : null));
                }
            } else {
                mainArgs.add(mainArgs.size(), arg);
            }
        }
    }

    public CmdLineArgs() {
    }

    public Map<String, String> getAllArgs() {
        return allArgs;
    }

    public Map<String, String> getLongArgs() {
        return longArgs;
    }

    public Map<String, String> getShortArgs() {
        return shortArgs;
    }

    public ArrayList<String> getMainArgs() {
        return mainArgs;
    }

    public Map<String, String[]> getExpectedArgs() {
        return expectedArgs;
    }

}
