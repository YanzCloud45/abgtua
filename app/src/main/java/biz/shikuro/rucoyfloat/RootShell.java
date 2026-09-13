package biz.shikuro.rucoyfloat;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public final class RootShell {
    private RootShell() {}

    public static String exec(String command) {
        StringBuilder out = new StringBuilder();
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                out.append(line).append('\n');
            }

            BufferedReader er = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));
            while ((line = er.readLine()) != null) {
                out.append(line).append('\n');
            }

            process.waitFor();
        } catch (Throwable t) {
            out.append("ERROR: ").append(t);
        } finally {
            if (process != null) process.destroy();
        }
        return out.toString().trim();
    }

    public static boolean available() {
        String result = exec("id");
        return result.contains("uid=0");
    }
}
