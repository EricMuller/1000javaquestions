import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.itextpdf.kernel.geom.LineSegment;
import com.itextpdf.kernel.geom.Vector;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.ITextExtractionStrategy;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by webdev on 13/05/17.
 */
public class JavaQuestions2017Extraction {

    private String titles[] = {
            "Java Basics",
            "OOPS",
            "Inheritance",
            "Static",
            "Method Overloading and Overriding",
            "Polymorphism",
            "Abstraction",
            "Final",
            "Package",
            "Internationalization",
            "Serialization",
            "Reflection",
            "Garbage Collection",
            "Inner Classes",
            "String",
            "Exception Handling",
            "Java Collection",
            "Java 8",
            "Multi-threading",
            "Java Tricky Questions",
            "Mixed Questions",
            "JSP",
            "Java Design Patterns",
            "Spring",
            "Hibernate",
            "Maven",
            "GIT",
            "AWS",
            "Microservices"};

    public JavaQuestions2017Extraction() {
    }

    public static void main(String[] args) throws Exception {
        List<Chunk> questions = new JavaQuestions2017Extraction().extractQuestionResponses("1000Javaquestions2017.pdf");

        ObjectMapper mapper = new ObjectMapper();

        mapper.writeValue(new File("1000Javaquestions2017.json"), questions);

    }

    private List<Chunk> extractQuestionResponses(String fileName) throws Exception {
        List<Chunk> questions = Lists.newArrayList();
        InputStream resourceStream = getClass().getResourceAsStream(fileName);
        try {
            String content = readDocumentAsStr(resourceStream);
            // extract  title and start position
            List<Chunk> categories = extractCategoriesPosition(content);
            // update end position for categories
            updateEndPositionAndContent(categories,null, content, false);
            // extract questions
            int currentPosition = extractQuestionsPosition(content, questions, 0, 503,true);
            // number question restart in book ...
            extractQuestionsPosition(content, questions, currentPosition, 497,false);
            //update last position  for question
            updateEndPositionAndContent(questions, categories, content, true);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(resourceStream);
        }
        return questions;
    }

    private String readDocumentAsStr(InputStream resourceStream) throws java.io.IOException {

        PdfDocument pdfDoc = new PdfDocument(new PdfReader(resourceStream));
        StringBuffer document = new StringBuffer();
        for (int i = 57; i <= pdfDoc.getNumberOfPages(); i++) {
            ITextExtractionStrategy strategy = new MySimpleTextExtractionStrategy();
            String page = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i), strategy);
            document.append(page);
        }
        pdfDoc.close();
        return document.toString();
    }

    private List<Chunk> extractCategoriesPosition(String content) {
        List<Chunk> categories = Lists.newArrayList();
        for (String title : titles) {
            int pos = content.indexOf(title + "\n");
            if(title.equals("Hibernate")){
                pos = content.indexOf(title + "\n",pos+1);
            }

            if (pos >= 0) {
                Chunk chunk = new Chunk();
                chunk.setLibelle(title);
                chunk.setStart(pos);
                categories.add(chunk);
            }
        }
        return categories;
    }

    private int extractQuestionsPosition(String content, List<Chunk> questions, int currentPosition, int nb,boolean jump) {
        int startPosition = currentPosition;
        int max = questions.size();
        for (int j = 1; j <= nb; j++) {
            int pos = content.indexOf(String.valueOf(j) + ".", startPosition);
            if(jump && j == 4){
                //quick fix double 4.
                 pos = content.indexOf(String.valueOf(j) + ".", pos+1);
            }
            if (pos >= 0) {
                Chunk chunk = new Chunk();
                System.out.println(String.valueOf(max + j) );
                System.out.println(String.valueOf(j) );
                chunk.setNumber(max + j);
                chunk.setLibelle("Question " + String.valueOf(max + j));
                chunk.setStart(pos);
                questions.add(chunk);
                startPosition = pos+1;
            } else {
                System.out.println(String.valueOf(j) + ". not found!!!");
            }
        }
        return startPosition;
    }

    private void updateEndPositionAndContent(List<Chunk> questions, List<Chunk> categories, String content, boolean isQuestion) {
        //sort start position
        questions.sort((c, c2) -> (c.getStart().compareTo(c2.getStart())));
        Chunk previous = null;
        for (Chunk chunk : questions) {
            if (previous != null) {
                previous.setEnd(chunk.getStart() - 1);
                if (isQuestion) {
                    if (previous.getStart() >= 0) {
                        updateQuestionContent(previous, categories, content);
                    }
                }
            }
            previous = chunk;
        }
        Chunk last = Iterables.getLast(questions, null);
        if (last != null) {
            last.setEnd(content.indexOf("THANKS"));
            if (isQuestion) {
                updateQuestionContent(last, categories, content);
            }
        }
    }

    private void updateQuestionContent(Chunk question, List<Chunk> categories, String document){
        String content = document.substring(question.getStart(), question.getEnd());
        int posInt = content.indexOf("?");
        if(question.getNumber() == 677){
            posInt = content.indexOf(")");
            posInt= posInt + 1;
        }

        if (posInt > 0) {
            int pos = content.indexOf(".");
            String q = content.substring(pos + 1, posInt + 1).trim().replace("\n", " ");
            String response = content.substring(posInt + 1).trim();
            question.setLibelle(q);
            question.setContent(response);
            for (Chunk categorie: categories) {
                 if(question.getStart() >= categorie.getStart() && question.getStart()<= categorie.getEnd()){
                       question.setCategorie(categorie.getLibelle());
                       break;
                 }
            }
            log(question);
        }else{
            System.out.print("? not found !! ");
            System.out.print(content);

        }
    }

    private void log(Chunk chunk) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("---------------------------------------").append("\n");
        stringBuffer.append(String.valueOf(chunk.getNumber())).append("\n");
        stringBuffer.append(chunk.getCategorie()).append("\n");
        stringBuffer.append(chunk.getLibelle()).append("\n");
        stringBuffer.append("---------------------------------------").append("\n");
        stringBuffer.append(chunk.getContent()).append("\n");
        System.out.println(stringBuffer.toString());
    }

    public class MySimpleTextExtractionStrategy implements ITextExtractionStrategy {
        private final StringBuilder result = new StringBuilder();
        private Vector lastStart;
        private Vector lastEnd;

        public MySimpleTextExtractionStrategy() {
        }

        public void eventOccurred(IEventData data, EventType type) {
            if (type.equals(EventType.RENDER_TEXT)) {
                TextRenderInfo renderInfo = (TextRenderInfo) data;
                boolean firstRender = this.result.length() == 0;
                boolean hardReturn = false;
                LineSegment segment = renderInfo.getBaseline();
                Vector start = segment.getStartPoint();
                Vector end = segment.getEndPoint();
                if (!firstRender) {
                    Vector x1 = this.lastStart;
                    Vector x2 = this.lastEnd;
                    float dist = x2.subtract(x1).cross(x1.subtract(start)).lengthSquared() / x2.subtract(x1).lengthSquared();
                    float sameLineThreshold = 1.0F;
                    if (dist > sameLineThreshold) {
                        hardReturn = true;
                    }
                }

                if (hardReturn) {
                    this.appendTextChunk("\n");
                }
                /*else if(!firstRender && this.result.charAt(this.result.length() - 1) != 32 && renderInfo.getText().length() > 0 && renderInfo.getText().charAt(0) != 32) {
                    float spacing = this.lastEnd.subtract(start).length();
                    if(spacing > renderInfo.getSingleSpaceWidth() / 2.0F) {
                        this.appendTextChunk(" ");
                    }
                }*/

                this.appendTextChunk(renderInfo.getText());
                this.lastStart = start;
                this.lastEnd = end;
            }
        }

        public Set<EventType> getSupportedEvents() {
            return Collections.unmodifiableSet(new LinkedHashSet(Collections.singletonList(EventType.RENDER_TEXT)));
        }

        public String getResultantText() {
            return this.result.toString();
        }

        protected final void appendTextChunk(CharSequence text) {
            this.result.append(text);
        }
    }

    private class Chunk {
        private int number;
        private String categorie;
        @JsonProperty("question")
        private String libelle;
        @JsonIgnore
        private Integer start;
        @JsonIgnore
        private Integer end;
        @JsonProperty("reponse")
        private String content;

        public String getLibelle() {
            return libelle;
        }

        public void setLibelle(String libelle) {
            this.libelle = libelle;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Integer getStart() {
            return start;
        }

        public void setStart(Integer start) {
            this.start = start;
        }

        public Integer getEnd() {
            return end;
        }

        public void setEnd(Integer end) {
            this.end = end;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public String getCategorie() {
            return categorie;
        }

        public void setCategorie(String categorie) {
            this.categorie = categorie;
        }
    }
}
