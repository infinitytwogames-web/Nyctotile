package org.infinitytwo.umbralore.core.manager;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.infinitytwo.umbralore.core.Main.cleanup;

public class CrashHandler {
    private static final List<String> crashMessages = Collections.synchronizedList(new ArrayList<>());
    private static final ConcurrentLinkedQueue<String> concerns = new ConcurrentLinkedQueue<>();

    static {
        buildText();
    }
    
    public static void init() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("\nUNCAUGHT EXCEPTION IN THREAD \"" + t.getName() + "\": " + e);
            e.printStackTrace();

            File crashDir = new File("crash-reports");
            if (!crashDir.exists()) crashDir.mkdirs();

            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            File crashFile = new File(crashDir, "crash-umbralore-" + timestamp + ".log");

            try (PrintWriter writer = new PrintWriter(new FileWriter(crashFile))) {
                // Header
                writer.println("============ Umbralore Crash Report ============");
                writer.println(randomCrashMessage());
                writer.println("\n--- Context ---");
                writer.println("Thread: " + t.getName());
                writer.println("Exception: " + e.getClass().getName());
                writer.println("Message: " + e.getMessage());
                writer.println();

                // Stack trace
                writer.println("--- Stack Trace ---");
                writer.print(formatStacktrace(e));

                // Concerns
                if (!concerns.isEmpty()) {
                    writer.println("\n--- Concerns ---");
                    concerns.forEach(writer::println);
                }

                // JVM Info
                writer.println("\n--- JVM Memory Info ---");
                writer.println("Total: " + toMB(Runtime.getRuntime().totalMemory()) + " MB");
                writer.println("Free : " + toMB(Runtime.getRuntime().freeMemory()) + " MB");
                writer.println("Max  : " + toMB(Runtime.getRuntime().maxMemory()) + " MB");

                // System Info
//                writer.println("\n--- System Properties ---");
//                System.getProperties().forEach((k, v) -> writer.println(k + ": " + v));

            } catch (IOException ex) {
                System.err.println("Failed to write crash report: " + ex);
            }

            System.err.println("Crash report saved to: " + crashFile.getAbsolutePath());
            cleanup();
        });
    }

    private static String randomCrashMessage() {
        if (crashMessages.isEmpty()) return "Oops! The game crashed unexpectedly.";
        return crashMessages.get(new Random().nextInt(crashMessages.size())) + "\n\nJokes aside...";
    }

    public static void addConcern(String message) {
        concerns.add(message);
    }

    private static long toMB(long bytes) {
        return bytes / 1024 / 1024;
    }
    
    public static String formatStacktrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    public static void buildText() {
        if (!crashMessages.isEmpty()) return;

        Collections.addAll(crashMessages,
                "Looks like your magic is too much for the game.",
                "The ghosts in the machine got hungry... for RAM.",
                "Hmmm, did you forget the semi-colon?",
                "Users are the best harsh testers!",
                "Fix me, please!",
                "Untrusted mod? Remove it then!",
                "Please look into my report. :>",
                "I wish I was efficient!",
                "Lore lies in the report.",
                "NullPointerException is random. :(",
                "Sowry :3",
                "God Damn, you are that evil?",
                "This chunk seems off...",
                "YEEEAHHHH. Oh sorry, I didn't realize you are here...",
                "OH MY PCeeeeeeee-",
                "That class is sus.",
                "Looks like shaders are doing jazz again...",
                "Error 404: Stability not found.",
                "Oopsie daisy, the world collapsed again.",
                "You've angered the Render Gods.",
                "Was it the particles? It's always the particles.",
                "Patience is the key for everything, including my door.",
                "Sir, i don't trust you.",
                "Trust me. This ain't Minecraft",
                "Hello, I am Min- I mean Umbralore. What do you want?",
                "Github Gods will hate me.",
                "You're not supposed to do that. Welcome to the wall of text of disappointment.",
                "Oops. That is a little embarrassing...",
                "Loading... or maybe not.",
                "I swear this worked yesterday.",
                "There is no bug, just features from the void.",
                "You think you're in control?",
                "Even magic has runtime errors.",
                "If you report this, pretend I wasn't here.",
                "The void reached back.",
                "Reality index out of bounds.",
                "This isn’t a bug, it’s a prophecy.",
                "Too much chaos magic at once?",
                "Syntax error in the spellbook.",
                "Are you trying to divide by fate?",
                "Thread of destiny terminated unexpectedly.",
                "This world needs a patch... literally.",
                "The code demons are laughing.",
                "Recursive summoning ritual failed.",
                "Don't worry. The dragons caused this one.",
                "Stack Overflow? Sounds like potion brewing.",
                "You cast Summon Crash. It worked.",
                "Uhhh... this wasn't in the simulation.",
                "You were never meant to see this.",
                "Every world has a breaking point.",
                "Alignment shifted. Try again later.",
                "You've entered the realm of undefined behavior.",
                "You broke the fourth wall. Happy?",
                "This is why we can't have nice things.",
                "Achievement Unlocked: Reality Breaker.",
                "The debugger ran away screaming.",
                "The stack trace looks like a summoning circle.",
                "Congrats! You found Schrödinger's bug.",
                "That method went on a coffee break.",
                "Your code tried to contact another dimension.",
                "Welcome to the land of forgotten brackets.",
                "Exception in thread 'main'... and all other threads.",
                "A wizard did it. No, really.",
                "The logs are just ancient scrolls now.",
                "Segfault? In my enchanted forest?",
                "You angered the compiler. Say sorry.",
                "That loop went on a spiritual journey.",
                "Your entity ascended to Nullheaven.",
                "Quantum variables detected. Schrödinger's null.",
                "Your crash has been lovingly handcrafted.",
                "System.out.println('oops')",
                "If this was intentional, you're a genius. If not... still impressive.",
                "Error: Brain not found.",
                "It compiled. That's the scary part.",
                "OMG, please work for some time.... I mean forever",
                "Do you need a therapy?",
                "Hello, I am your lawyer. Why did you kill \"Umbralore\"?",
                "The void says: Try again.",
                "A wild exception appeared!",
                "Your spell fizzled... violently.",
                "One does not simply run MysticalEngine without errors.",
                "You've broken causality. Good job.",
                "Did you just mod in a black hole?",
                "I'm too powerful to fail. And yet...",
                "Let me just blame Java real quick...",
                "This isn't a bug, it's performance art.",
                "Who let the Nulls out?",
                "My constructor wasn’t ready for this.",
                "Tip: Sacrifice fewer goats next time.",
                "Turns out magic and logic don’t mix.",
                "Garbage collector gave up. It’s meditating.",
                "Unhandled exception, like my emotions.",
                "Caused by: SpaghettiCodeException",
                "Don’t panic. Just run.",
                "The magic circle was drawn... incorrectly.",
                "Okay, but have you *tried* turning it off and on again?",
                "I don't know what happened either. Let’s just blame the shaders.",
                "Look away. I’m not decent.",
                "You stepped on a bug, and it fought back.",
                "Definitely not a side quest."
                );
    }
}
