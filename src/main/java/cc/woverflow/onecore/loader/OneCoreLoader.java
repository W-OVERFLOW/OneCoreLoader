package cc.woverflow.onecore.loader;

import cc.woverflow.onecore.loader.utils.StringBuilderWriter;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.essential.loader.stage0.EssentialSetupTweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.function.Supplier;

public class OneCoreLoader extends EssentialSetupTweaker {

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

    public static String getChecksumOfFile(String filename) {
        try (FileInputStream inputStream = new FileInputStream(filename)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytesBuffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
                digest.update(bytesBuffer, 0, bytesRead);
            }

            return convertByteArrayToHexString(digest.digest());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuilder stringBuffer = new StringBuilder();
        for (byte arrayByte : arrayBytes) {
            stringBuffer.append(Integer.toString((arrayByte & 0xff) + 0x100, 16)
                    .substring(1));
        }
        return stringBuffer.toString();
    }

    private boolean download(String url, File file) {
        url = url.replace(" ", "%20");
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            try (BufferedInputStream in = new BufferedInputStream(setupConnection(url))) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = (in.read(buffer, 0, 1024))) > 0) {
                    fileOut.write(buffer, 0, read);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        super.injectIntoClassLoader(classLoader);
        File loadLocation = new File(new File(new File(Launch.minecraftHome, "W-OVERFLOW"), "OneCore"), "OneCore.jar");
        JsonObject json = null;
        boolean deobf = ((boolean) Launch.blackboard.getOrDefault("fml.deobfuscatedEnvironment", false));
        try {
            if (!loadLocation.getParentFile().exists()) loadLocation.getParentFile().mkdirs();
            Supplier<String> supplier = () -> {
                try (InputStreamReader input = new InputStreamReader(setupConnection("https://woverflow.cc/static/data/onecore.json"), Charset.defaultCharset())) {
                    StringBuilderWriter builder = new StringBuilderWriter();
                    char[] buffer = new char[4096];
                    int n;
                    while ((n = input.read(buffer)) > 0) {
                        builder.write(buffer, 0, n);
                    }
                    return builder.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                    showErrorScreen();
                    return "";
                }
            };
            json = new JsonParser().parse(supplier.get()).getAsJsonObject();
            if (json.has("core")) {
                if (!loadLocation.exists() || (!getChecksumOfFile(loadLocation.getPath()).equals(json.get(deobf ? "checksum_core_dev" : "checksum_core").getAsString()))) {
                    System.out.println("Downloading / updating OneCore...");
                    if (!download(json.get(deobf ? "core_dev" : "core").getAsString(), loadLocation)) {
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
                if (!deobf) { //ideally, this wouldn't be needed, but compileOnly doesn't work with forge for some reason.
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

    private InputStream setupConnection(String url) throws IOException {
        HttpURLConnection connection = ((HttpURLConnection) new URL(url).openConnection());
        connection.setRequestMethod("GET");
        connection.setUseCaches(false);
        connection.addRequestProperty("User-Agent", "OneCoreLoader/1.2.1");
        connection.setReadTimeout(5000);
        connection.setConnectTimeout(5000);
        connection.setDoOutput(true);
        return connection.getInputStream();
    }
}
