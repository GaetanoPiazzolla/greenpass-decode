package com.tano.green;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

import java.util.zip.Inflater;

import COSE.Encrypt0Message;
import COSE.Message;
import com.google.iot.cbor.CborMap;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import nl.minvws.encoding.Base45;

import javax.imageio.ImageIO;

public class Main {
    private static final int BUFFER_SIZE = 1024;

    public static void main(String args[]) throws Exception{

        // 1 - read text from file
        File file = new File("green-pass.jpg");
        BufferedImage bufferedImage = ImageIO.read(file);
        LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Result result = new MultiFormatReader().decode(bitmap);
        String text = result.getText();

        // 2 - remove prefix "HC1:" and decode base45 string
        byte[] bytecompressed = Base45.getDecoder().decode(text.substring(4));

        // 3 - inflate string using zlib
        Inflater inflater = new Inflater();
        inflater.setInput(bytecompressed);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(bytecompressed.length);
        byte[] buffer = new byte[BUFFER_SIZE];
        while (!inflater.finished()) {
            final int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }

        // 4 - decode COSE message (no signature verification done)
        Message a = Encrypt0Message.DecodeFromBytes(outputStream.toByteArray());

        // 5 create CborObject MAP
        CborMap cborMap = CborMap.createFromCborByteArray(a.GetContent());
        System.out.println(cborMap.toString(2));
    }


}
