/*
 *
 * Copyright (C) 2017 Tres Finocchiaro, QZ Industries
 * Copyright (C) 2017 Yao Chunlin
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */
package qz.printer;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.ByteArrayBuilder;
import qz.exception.InvalidRawImageException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Abstract wrapper for pdf to be printed with thermal printers.
 *
 * @author Yao Chunlin
 */
@SuppressWarnings("UnusedDeclaration") //Library class
public class PDF2EPLWrapper {


    public void setCropType(CropType cropType) {
        this.cropType = cropType;
    }

    public enum CropType {
        TOP, BOTTOM, NONE, BOTH;
    }
    private static final Logger log = LoggerFactory.getLogger(PDF2EPLWrapper.class);
    private final LanguageType languageType;
    private final PDDocument pdfdoc;
    private int dpi;
    private Charset charset = Charset.defaultCharset();
    private int xPos = 0;   // X coordinate used for EPL2, CPCL.  Irrelevant for ZPLII, ESC/P, etc
    private int yPos = 0;   // Y coordinate used for EPL2, CPCL.  Irrelevant for ZPLII, ESC/P, etc
    private int dotDensity = 32;  // Generally 32 = Single (normal) 33 = Double (higher res) for ESCP.  Irrelevant for all other languages.
    private ByteArrayBuilder byteBuffer = new ByteArrayBuilder();
    private CropType cropType = CropType.NONE;
    /**
     * Creates a new
     * <code>PDF2EPLWrapper</code> from a
     * <code>PDDocument.</code>
     *
     * @param pdfdoc The PDF document to convert for thermal printing
     */
    public PDF2EPLWrapper(PDDocument pdfdoc, LanguageType languageType) {
        this.pdfdoc = pdfdoc;
        this.languageType = languageType;
        log.info("Loading PDF document with {} pages", pdfdoc.getNumberOfPages());

    }

    /**
     * Generates the EPL2 commands to print an image. One command is emitted per
     * line of the image. This avoids issues with commands being too long.
     *
     * @return The commands to print the pdf as an array of bytes, ready to be
     * sent to the printer
     */
    public byte[] getImageCommand(JSONObject opt) throws InvalidRawImageException, IOException {
        getByteBuffer().clear();
        int numberOfPages = pdfdoc.getNumberOfPages();
        PDFRenderer pdfRenderer = new PDFRenderer(pdfdoc);
        for (int pageIndex = 0; pageIndex < numberOfPages; pageIndex ++) {
            BufferedImage bim = pdfRenderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB);
            bim = getCroppedImage(bim, cropType);
            getByteBuffer().append(getImageWrapper(bim, pageIndex).getImageCommand(opt));
        }
        return getByteBuffer().getByteArray();
    }

    public BufferedImage getCroppedImage(BufferedImage source, CropType cropType) {
        if (cropType == CropType.NONE) {
            return source;
        }
        // Get our top-left pixel color as our "baseline" for cropping
        int baseColor = Color.white.getRGB();

        int width = source.getWidth();
        int height = source.getHeight();

        int topY = 0, bottomY = height - 1;
        if(cropType == CropType.TOP || cropType == CropType.BOTH) {
            for(int y=0; y<height; y++) {
                boolean blankRow = true;
                for(int x=0; x<width; x++) {
                    if (baseColor != source.getRGB(x, y)) {
                        blankRow = false;
                        break;
                    }
                }
                if(blankRow == true) {
                    continue;
                } else {
                    topY = y;
                    break;
                }
            }
        }

        if(cropType == CropType.BOTTOM || cropType == CropType.BOTH) {
            for(int y=height-1; y>=0; y--) {
                boolean blankRow = true;
                for(int x=0; x<width; x++) {
                    if (baseColor != source.getRGB(x, y)) {
                        blankRow = false;
                        break;
                    }
                }
                if(blankRow == true) {
                    continue;
                } else {
                    bottomY = y;
                    break;
                }
            }
        }


        //BufferedImage destination = new BufferedImage( width,
        //                                               (bottomY - topY + 1), BufferedImage.TYPE_INT_ARGB);
        BufferedImage destination = source.getSubimage(0, topY, width, (bottomY-topY + 1));

        //destination.getGraphics().drawImage(source, 0, 0,
        //                                    destination.getWidth(), destination.getHeight() ,
        //                                    0, topY, width - 1, bottomY, null);

        return destination;
    }

    private ImageWrapper getImageWrapper(BufferedImage img, int pageIndex) {
        ImageWrapper iw = new ImageWrapper(img, languageType);
        iw.setCharset(charset);

        //ESCP only
        iw.setDotDensity(dotDensity);

        //EPL only
        iw.setxPos(xPos);
        if (pageIndex == 0) {
            //Set yPos only for first page.
            iw.setyPos(yPos);
        }
        return iw;
    }

    /**
     * buffer
     *
     * @return
     */
    public ByteArrayBuilder getByteBuffer() {
        return byteBuffer;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public void setDotDensity(int dotDensity) {
        this.dotDensity = dotDensity;
    }

    public void setxPos(int xPos) {
        this.xPos = xPos;
    }

    public void setyPos(int yPos) {
        this.yPos = yPos;
    }

    public void setDPI(int dpi) {
        this.dpi = dpi;
    }
}