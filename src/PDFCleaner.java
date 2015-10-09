import java.io.FileNotFoundException;
import java.util.Date;

import org.pdfclown.documents.Document;
import org.pdfclown.documents.Page;
import org.pdfclown.documents.contents.ContentScanner;
import org.pdfclown.documents.contents.Contents;
import org.pdfclown.documents.contents.composition.PrimitiveComposer;
import org.pdfclown.documents.contents.objects.ContentObject;
import org.pdfclown.documents.contents.objects.LocalGraphicsState;
import org.pdfclown.documents.contents.objects.Path;
import org.pdfclown.documents.contents.objects.SetFillColor;
import org.pdfclown.documents.contents.objects.Text;
import org.pdfclown.documents.contents.objects.XObject;
import org.pdfclown.documents.interaction.viewer.ViewerPreferences;
import org.pdfclown.documents.interchange.metadata.Information;
import org.pdfclown.files.File;
import org.pdfclown.files.SerializationModeEnum;
import org.pdfclown.objects.PdfInteger;
import org.pdfclown.documents.contents.colorSpaces.DeviceRGBColor;

public class PDFCleaner {
	public static boolean DEBUG = false;
	
	private static String inputPath = "c:/users/n11093/desktop/Les2.pdf";
	private static String outputPath = "c:/users/n11093/desktop/Les2.wit.pdf";
	
	public static void main(String[] args) throws FileNotFoundException {
		if (args.length > 0) {
			inputPath = args[0];
			outputPath = args[1];
			DEBUG = args.length > 2;
		}
		
		File file = new File(inputPath);
		Document document = file.getDocument();
		
		int nrPages = document.getNumberOfPages();
		for (int p = 0; p < nrPages; p++) {
			System.out.println("Processing page: "+p);
			Page page = document.getPages().get(p);
			Contents pageContents = page.getContents();
			ContentScanner scanner = new ContentScanner(pageContents);
			process(scanner, page, 1);
			pageContents.flush();
		}
		System.out.println("Done, now saving");
		serialize(file, outputPath);
	}
	
	private static void process(ContentScanner level, Page page, int l) {
		if (level == null)
			return;
		PrimitiveComposer builder = new PrimitiveComposer(level);
	
		while (level.moveNext()) {
			ContentObject object = level.getCurrent();
			if (DEBUG)
				System.err.println(l+": "+"ContentObject: "+object.getClass()+"     "+object.toString());
			process(level.getChildLevel(), page, l+1);
			if (object instanceof Path) {
				builder.setFillColor(new DeviceRGBColor(1,1,1));
			} else if(object instanceof Text) {
				builder.setFillColor(new DeviceRGBColor(0,0,0));
			} else if(level.getParent() instanceof Text && object instanceof SetFillColor) {
				SetFillColor t = (SetFillColor) object;
				t.getComponents().clear();
				t.getComponents().add(new PdfInteger(0));
				t.getComponents().add(new PdfInteger(0));
				t.getComponents().add(new PdfInteger(0));
			} else if(level.getParent() instanceof Path && object instanceof SetFillColor) {
				SetFillColor t = (SetFillColor) object;
				t.getComponents().clear();
				t.getComponents().add(new PdfInteger(1));
				t.getComponents().add(new PdfInteger(1));
				t.getComponents().add(new PdfInteger(1));
			} else if(level.getParent() instanceof LocalGraphicsState && object instanceof XObject) {
				XObject t = (XObject) object;
				LocalGraphicsState p = (LocalGraphicsState) level.getParent();
				if (!p.getObjects().get(0).toString().contains("[2038")) continue;
				t.getObjects().clear();
			}
		}
	}

	private static void applyDocumentSettings(Document document) {
		ViewerPreferences view = new ViewerPreferences(document);
		document.setViewerPreferences(view);
		view.setDisplayDocTitle(true);
		Information info = document.getInformation();
		info.setCreationDate(new Date());
		info.setCreator("PDF Cleaner");
	}
	
	private static String serialize(File file, String fileName) {
		applyDocumentSettings(file.getDocument());
		SerializationModeEnum serializationMode = SerializationModeEnum.Standard;
		java.io.File outputFile = new java.io.File(fileName);
		try {
			file.save(outputFile, serializationMode);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return outputFile.getPath();
	}
	

}
