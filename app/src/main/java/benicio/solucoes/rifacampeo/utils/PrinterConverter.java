package benicio.solucoes.rifacampeo.utils;

import android.graphics.Bitmap;

import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

import java.util.EnumMap;

public class PrinterConverter {

    public static class PrinterException extends Exception {
        public PrinterException(String message) {
            super(message);
        }
    }


    private static byte[] initImageCommand(int bytesByLine, int bitmapHeight) {
        int
                xH = bytesByLine / 256,
                xL = bytesByLine - (xH * 256),
                yH = bitmapHeight / 256,
                yL = bitmapHeight - (yH * 256);

        byte[] imageBytes = new byte[8 + bytesByLine * bitmapHeight];
        System.arraycopy(new byte[]{0x1D, 0x76, 0x30, 0x00, (byte) xL, (byte) xH, (byte) yL, (byte) yH}, 0, imageBytes, 0, 8);
        return imageBytes;
    }

    public static byte[] bitmapToBytes(Bitmap bitmap) {
        int
                bitmapWidth = bitmap.getWidth(),
                bitmapHeight = bitmap.getHeight(),
                bytesByLine = (int) Math.ceil(((float) bitmapWidth) / 8f);

        byte[] imageBytes = PrinterConverter.initImageCommand(bytesByLine, bitmapHeight);

        int i = 8;
        for (int posY = 0; posY < bitmapHeight; posY++) {
            for (int j = 0; j < bitmapWidth; j += 8) {
                StringBuilder stringBinary = new StringBuilder();
                for (int k = 0; k < 8; k++) {
                    int posX = j + k;
                    if (posX < bitmapWidth) {
                        int color = bitmap.getPixel(posX, posY),
                                r = (color >> 16) & 0xff,
                                g = (color >> 8) & 0xff,
                                b = color & 0xff;

                        if (r > 160 && g > 160 && b > 160) {
                            stringBinary.append("0");
                        } else {
                            stringBinary.append("1");
                        }
                    } else {
                        stringBinary.append("0");
                    }
                }
                imageBytes[i++] = (byte) Integer.parseInt(stringBinary.toString(), 2);
            }
        }

        return imageBytes;
    }


    public static byte[] QRCodeDataToBytes(String data, int size) throws PrinterException {

        ByteMatrix byteMatrix = null;

        try {
            EnumMap<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCode code = Encoder.encode(data, ErrorCorrectionLevel.L, hints);
            byteMatrix = code.getMatrix();

        } catch (WriterException e) {
            e.printStackTrace();
            throw new PrinterException("Unable to encode QR code");
        }

        if (byteMatrix == null) {
            return PrinterConverter.initImageCommand(0, 0);
        }

        int width = byteMatrix.getWidth(),
                height = byteMatrix.getHeight(),
                coefficient = Math.round((float) size / (float) width),
                imageWidth = width * coefficient,
                imageHeight = height * coefficient,
                bytesByLine = (int) Math.ceil(((float) imageWidth) / 8f),
                i = 8;

        if (coefficient < 1) {
            return PrinterConverter.initImageCommand(0, 0);
        }

        byte[] imageBytes = PrinterConverter.initImageCommand(bytesByLine, imageHeight);

        for (int y = 0; y < height; y++) {
            byte[] lineBytes = new byte[bytesByLine];
            int x = -1, multipleX = coefficient;
            boolean isBlack = false;
            for (int j = 0; j < bytesByLine; j++) {
                StringBuilder stringBinary = new StringBuilder();
                for (int k = 0; k < 8; k++) {
                    if (multipleX == coefficient) {
                        isBlack = ++x < width && byteMatrix.get(x, y) == 1;
                        multipleX = 0;
                    }
                    stringBinary.append(isBlack ? "1" : "0");
                    ++multipleX;
                }
                lineBytes[j] = (byte) Integer.parseInt(stringBinary.toString(), 2);
            }

            for (int multipleY = 0; multipleY < coefficient; ++multipleY) {
                System.arraycopy(lineBytes, 0, imageBytes, i, lineBytes.length);
                i += lineBytes.length;
            }
        }

        return imageBytes;
    }
}

