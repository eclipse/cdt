package test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4e.cpp.language.cquery.*;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.*;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;

public class CqueryJsonParseTest {

	private void assertParse(final String json, final NotificationMessage expectedResult) {
		Gson gson = new Gson();
		NotificationMessage actualResult = new NotificationMessage();
		actualResult = gson.fromJson(json, NotificationMessage.class);
		Assert.assertEquals(expectedResult.toString(), actualResult.toString());
	}

	@Test
	public void testProgress(){
		String json = "{\"jsonrpc\": \"2.0\",\"method\": \"$cquery/progress\",\"params\": {" //$NON-NLS-1$
				+ "\"indexRequestCount\": 4,\"doIdMapCount\": 5,\"loadPreviousIndexCount\": 6," //$NON-NLS-1$
				+ "\"onIdMappedCount\": 7,\"onIndexedCount\": 8,\"activeThreads\": 9}}"; //$NON-NLS-1$

		IndexingProgressStats expectedIndex = new IndexingProgressStats(4, 5, 6, 7, 8, 9);
		NotificationMessage expectedResult = new NotificationMessage();
		expectedResult.setJsonrpc("2.0"); //$NON-NLS-1$
		expectedResult.setMethod("$cquery/progress"); //$NON-NLS-1$
		expectedResult.setParams(expectedIndex);
		assertParse(json, expectedResult);
	}

	@Test
	public void testSetInactiveRegions() {
		String json = "{\"jsonrpc\": \"2.0\",\"method\": \"$cquery/setInactiveRegions\",\"params\": {" //$NON-NLS-1$
				+ "\"uri\": \"file:///home/foobar.cpp\",\"inactiveRegions\": []}}";  //$NON-NLS-1$

		URI uri = URI.create("file:///home/foobar.cpp"); //$NON-NLS-1$
		List<Range> regions = new ArrayList<>();
		CqueryInactiveRegions expectedRegions = new CqueryInactiveRegions(uri, regions);

		NotificationMessage expectedResult = new NotificationMessage();
		expectedResult.setJsonrpc("2.0"); //$NON-NLS-1$
		expectedResult.setMethod("$cquery/setInactiveRegions"); //$NON-NLS-1$
		expectedResult.setParams(expectedRegions);
		assertParse(json, expectedResult);
	}

	@Test
	public void testPublishSemanticHighlighting() {
		String json = "{\"jsonrpc\": \"2.0\",\"method\": \"$cquery/publishSemanticHighlighting\"," //$NON-NLS-1$
				+ "\"params\": {\"uri\": \"file:///home/foobar.cpp\",\"symbols\": [{\"stableId\": 21," //$NON-NLS-1$
				+ "\"parentKind\": 8,\"kind\": 0,\"storage\": 3,\"ranges\": [{\"start\": {\"line\": 41,"  //$NON-NLS-1$
				+ "\"character\": 1},\"end\": {\"line\": 41,\"character\": 5}}]},{\"stableId\": 19,"  //$NON-NLS-1$
				+ "\"parentKind\": 12,\"kind\": 253,\"storage\": 5,\"ranges\": [{\"start\": {\"line\": 39,"  //$NON-NLS-1$
				+ "\"character\": 9},\"end\": {\"line\": 39,\"character\": 10}}]}]}}"; //$NON-NLS-1$

		URI uri = URI.create("file:///home/foobar.cpp"); //$NON-NLS-1$
		Position pos1 = new Position(41, 1);
		Position pos2 = new Position(41, 5);
		Position pos3 = new Position(39, 9);
		Position pos4 = new Position(39, 10);
		Range range1 = new Range(pos1,pos2);
		Range range2 = new Range(pos3,pos4);
		List<Range> ranges1 = new ArrayList<>();
		List<Range> ranges2 = new ArrayList<>();
		ranges1.add(range1);
		ranges2.add(range2);
		ExtendedSymbolKindType parentKind1 = new ExtendedSymbolKindType(8);
		ExtendedSymbolKindType parentKind2 = new ExtendedSymbolKindType(12);
		ExtendedSymbolKindType kind1 = new ExtendedSymbolKindType(0);
		ExtendedSymbolKindType kind2 = new ExtendedSymbolKindType(253);
		StorageClass storage1 = StorageClass.Static;
		StorageClass storage2 = StorageClass.Auto;
		HighlightSymbol symbol1 = new HighlightSymbol(21, parentKind1, kind1, storage1, ranges1);
		HighlightSymbol symbol2 = new HighlightSymbol(19, parentKind2, kind2, storage2, ranges2);
		List<HighlightSymbol> symbols = new ArrayList<>();
		symbols.add(symbol1);
		symbols.add(symbol2);
		CquerySemanticHighlights exceptedHighlights = new CquerySemanticHighlights(uri, symbols);

		NotificationMessage expectedResult = new NotificationMessage();
		expectedResult.setJsonrpc("2.0"); //$NON-NLS-1$
		expectedResult.setMethod("$cquery/publishSemanticHighlighting"); //$NON-NLS-1$
		expectedResult.setParams(exceptedHighlights);
		assertParse(json, expectedResult);
	}
}
