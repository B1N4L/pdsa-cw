import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

// Writes the app icon as ams.png (Linux), ams.ico (Windows) and icon-*.png (website), drawn by ams.UiKit.
//   java -cp <classes> packaging/tools/MakeIcons.java <output folder>
public class MakeIcons {

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : ".");
        out.mkdirs();
        ImageIO.write(icon(256), "png", new File(out, "ams.png"));
        ImageIO.write(icon(512), "png", new File(out, "icon-512.png"));
        ImageIO.write(icon(180), "png", new File(out, "icon-180.png"));
        ImageIO.write(icon(64), "png", new File(out, "icon-64.png"));

        // .ico with PNG images inside (supported since Windows Vista)
        int[] sizes = {16, 24, 32, 48, 64, 128, 256};
        List<byte[]> images = new ArrayList<>();
        for (int size : sizes) {
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(icon(size), "png", png);
            images.add(png.toByteArray());
        }
        ByteBuffer header = ByteBuffer.allocate(6 + 16 * sizes.length).order(ByteOrder.LITTLE_ENDIAN);
        header.putShort((short) 0).putShort((short) 1).putShort((short) sizes.length);
        int offset = 6 + 16 * sizes.length;
        for (int i = 0; i < sizes.length; i++) {
            header.put((byte) (sizes[i] >= 256 ? 0 : sizes[i])).put((byte) (sizes[i] >= 256 ? 0 : sizes[i]));
            header.put((byte) 0).put((byte) 0).putShort((short) 1).putShort((short) 32);
            header.putInt(images.get(i).length).putInt(offset);
            offset += images.get(i).length;
        }
        try (FileOutputStream ico = new FileOutputStream(new File(out, "ams.ico"))) {
            ico.write(header.array());
            for (byte[] image : images) {
                ico.write(image);
            }
        }
        System.out.println("Icons written to " + out);
    }

    private static BufferedImage icon(int size) throws Exception {
        return (BufferedImage) Class.forName("ams.UiKit").getMethod("appIcon", int.class).invoke(null, size);
    }
}
