package cc.woverflow.onecore.loader;

import cc.woverflow.onecore.loader.utils.InternetUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import javax.swing.*;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

public class OneCoreLoader implements ITweaker {

    private static void showErrorScreen() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JOptionPane.showMessageDialog(
                null,
                "OneCore has failed to download!\n" +
                        "This may be because the servers are down.\n" +
                        "For more information, please join our discord server: https://woverflow.cc/discord\n" +
                        "or try again later.",
                "OneCore has failed!", JOptionPane.ERROR_MESSAGE
        );
        try {
            Method exit = Class.forName("java.lang.Shutdown").getDeclaredMethod("exit", int.class);
            exit.setAccessible(true);
            exit.invoke(null, 1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {

    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        File loadLocation = new File(new File(new File(Launch.minecraftHome, "W-OVERFLOW"), "OneCore"), "OneCore.jar");
        JsonObject json = null;
        boolean deobf = ((boolean) Launch.blackboard.getOrDefault("fml.deobfuscatedEnvironment", false));
        try {
            if (!loadLocation.getParentFile().exists()) loadLocation.getParentFile().mkdirs();
            String theJson = InternetUtils.getStringOnline("https://woverflow.cc/static/data/onecore.json");
            if (theJson == null) {
                if (!loadLocation.exists()) {
                    showErrorScreen();
                } else {
                    try {
                        URL fileURL = loadLocation.toURI().toURL();
                        if (!Launch.classLoader.getSources().contains(fileURL)) {
                            Launch.classLoader.addURL(fileURL);
                        }
                        try {
                            ClassLoader parent = Launch.classLoader.getClass().getClassLoader();
                            if (!(parent instanceof URLClassLoader) || !Arrays.asList(((URLClassLoader) parent).getURLs()).contains(fileURL)) {
                                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                                method.setAccessible(true);
                                method.invoke(parent, fileURL);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Launch.classLoader.findClass("cc.woverflow.onecore.init.OneCoreInit").getDeclaredMethod("initialize").invoke(null);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        showErrorScreen();
                        return;
                    }
                }
                return;
            }
            json = new JsonParser().parse(theJson).getAsJsonObject();
            if (json.has("core")) {
                if (!loadLocation.exists() || (!InternetUtils.getChecksumOfFile(loadLocation.getPath()).equals(json.get(deobf ? "checksum_core_dev" : "checksum_core").getAsString()))) {
                    System.out.println("Downloading / updating OneCore...");
                    if (!InternetUtils.download(json.get(deobf ? "core_dev" : "core").getAsString(), loadLocation)) {
                        if (!loadLocation.exists()) {
                            showErrorScreen();
                        }
                    }
                }
            } else {
                // oh
                if (!loadLocation.exists()) {
                    showErrorScreen();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            if (!loadLocation.exists()) {
                showErrorScreen();
            }
        }

        if (json != null) {
            try {
                URL fileURL = loadLocation.toURI().toURL();
                if (!Launch.classLoader.getSources().contains(fileURL)) {
                    Launch.classLoader.addURL(fileURL);
                }
                try {
                    ClassLoader parent = Launch.classLoader.getClass().getClassLoader();
                    if (!(parent instanceof URLClassLoader) || !Arrays.asList(((URLClassLoader) parent).getURLs()).contains(fileURL)) {
                        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                        method.setAccessible(true);
                        method.invoke(parent, fileURL);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Throwable e) {
                e.printStackTrace();
                showErrorScreen();
                return;
            }
        }
        try {
            if (json != null) {
                Launch.classLoader.findClass(json.getAsJsonObject("classpath").get("main").getAsString()).getDeclaredMethod("initialize").invoke(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorScreen();
        }
    }

    @Override
    public String getLaunchTarget() {
        return null;
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }
}
