import org.bytedeco.javacpp.*;

import java.io.IOException;
import java.util.Scanner;


import static org.bytedeco.javacpp.opencv_core.IplImage;
import static org.bytedeco.javacpp.opencv_core.cvFlip;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvSaveImage;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.VideoInputFrameGrabber;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import static org.bytedeco.javacpp.lept.*;
import static org.bytedeco.javacpp.tesseract.*;

public class BasicExample {

    public static final String GOOGLE_SEARCH_URL = "https://www.google.com/search";

    public static void main(String[] args) {

        FrameGrabber grabber = new VideoInputFrameGrabber(0); // 1 for next camera
        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        IplImage img;
        int frame_number = 0;
        Frame frame = null;
        try {
            grabber.start();
            frame = grabber.grab();
        }
        catch (FrameGrabber.Exception ex) {
            System.out.println("Error grabbing frame: '"+ex.getMessage()+"'");
            ex.printStackTrace();
            System.exit(1);
        }
        img = converter.convert(frame);
        //the grabbed frame will be flipped, re-flip to make it right
        cvFlip(img, img, 1);// l-r = 90_degrees_steps_anti_clockwise

        //save
        cvSaveImage("HQframe"+(frame_number++)+".jpg",img);


        BytePointer outText;

        TessBaseAPI api = new TessBaseAPI();
        // Initialize tesseract-ocr with English, without specifying tessdata path
        if (api.Init(null, "eng") != 0) {
            System.err.println("Could not initialize tesseract.");
            System.exit(1);
        }

        // Open input image with leptonica library
        PIX image = pixRead(args.length > 0 ? args[0] : "C:/Users/Lincoln/Desktop/hq-trivia.jpg");
        api.SetImage(image);
        // Get OCR result
        outText = api.GetUTF8Text();
        String outString = outText.getString().trim();
        System.out.println("OCR output:\n" + outString);
        int splitPoint = outString.indexOf("\n\n");
        String question = outString.substring(0,splitPoint).replace('\n',' ');//.replace(' ','+');
        String[] answers = outString.substring(splitPoint + 2,outString.length()).split("\n", 3);
        System.out.println("HQ question: \n"+question+"\n\nHQ answers: ");
        for(String answer : answers) {
            //answer = answer.replace(' ','+');
            System.out.println("'"+answer+"'");
        }

        // Destroy used object and release memory
        api.End();
        outText.deallocate();
        pixDestroy(image);

        int num = 2;

        //https://www.google.com/search?as_q=Which+app+did+Apple+name+%27App+of+the+Year%27+for+2016%3F&as_epq=%22Facebook%22+OR+%22Got+Juice%3F%22+OR+%22Prisma%22&as_oq=&as_eq=&as_nlo=&as_nhi=&lr=&cr=&as_qdr=all&as_sitesearch=&as_occt=any&safe=images&as_filetype=&as_rights=

        String searchURL = GOOGLE_SEARCH_URL+"?q="+question+"&num="+num+"&as_oq=\""+answers[0]+"\"+OR+\""+answers[1]+"\"+OR+\""+answers[2]+"\"";
        System.out.println(searchURL);
        //without proper User-Agent, we will get 403 error
        String resultText = null;
        try {
            startTimer();
            Document doc = Jsoup.connect(searchURL).userAgent("Mozilla/5.0").get();
            resultText = doc.text();
            //System.out.println(doc.text());
            System.out.println("google search time: "+stopTimer());
        }
        catch (IOException ex) {
            System.out.println("IOException while getting google results: '"+ex.getMessage()+"'...");
            ex.printStackTrace();
            System.exit(1);
        }
        if(resultText!=null) {
            startTimer();
            int answer1_freq = count(resultText, answers[0]);
            int answer2_freq = count(resultText, answers[1]);
            int answer3_freq = count(resultText, answers[2]);
            System.out.println("count 3 answers time: "+stopTimer());
            System.out.println("count for '"+answers[0]+"': "+answer1_freq);
            System.out.println("count for '"+answers[1]+"': "+answer2_freq);
            System.out.println("count for '"+answers[2]+"': "+answer3_freq);
        }
    }


    private static int count(final String string, final String substring) {
        int count = 0;
        int idx = 0;
        while ((idx = string.indexOf(substring, idx)) != -1)
        {
            idx++;
            count++;
        }
        return count;
    }
}