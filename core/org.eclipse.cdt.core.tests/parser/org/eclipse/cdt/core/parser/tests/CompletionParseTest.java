/*
 * Created on Dec 8, 2003
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package org.eclipse.cdt.core.parser.tests;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.cdt.core.parser.ParserLanguage;
import org.eclipse.cdt.core.parser.ast.IASTClassSpecifier;
import org.eclipse.cdt.core.parser.ast.IASTCodeScope;
import org.eclipse.cdt.core.parser.ast.IASTCompletionNode;
import org.eclipse.cdt.core.parser.ast.IASTDeclaration;
import org.eclipse.cdt.core.parser.ast.IASTExpression;
import org.eclipse.cdt.core.parser.ast.IASTField;
import org.eclipse.cdt.core.parser.ast.IASTFunction;
import org.eclipse.cdt.core.parser.ast.IASTMethod;
import org.eclipse.cdt.core.parser.ast.IASTNamespaceDefinition;
import org.eclipse.cdt.core.parser.ast.IASTNode;
import org.eclipse.cdt.core.parser.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.parser.ast.IASTTypedefDeclaration;
import org.eclipse.cdt.core.parser.ast.IASTVariable;
import org.eclipse.cdt.core.parser.ast.IASTCompletionNode.CompletionKind;
import org.eclipse.cdt.core.parser.ast.IASTNode.ILookupResult;
import org.eclipse.cdt.core.parser.ast.IASTNode.LookupKind;

/**
 * @author jcamelon
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class CompletionParseTest extends CompletionParseBaseTest {

	
	public CompletionParseTest(String name) {
		super(name);
	}

	public void testBaseCase_SimpleDeclaration() throws Exception
	{
		StringWriter writer = new StringWriter(); 
		writer.write( "class ABC " );  //$NON-NLS-1$
		writer.write( "{int x;}; " );  //$NON-NLS-1$
		writer.write( "AB\n\n" ); //$NON-NLS-1$

		IASTCompletionNode node = null;
		Iterator keywords = null;
		
		node = parse( writer.toString(), 21); 
		assertNotNull( node );
		assertNotNull( node.getCompletionPrefix() );
		assertEquals( node.getCompletionScope(), ((Scope)callback.getCompilationUnit()).getScope() );
		assertEquals( node.getCompletionPrefix(), "A"); //$NON-NLS-1$
		assertEquals( node.getCompletionKind(), IASTCompletionNode.CompletionKind.VARIABLE_TYPE );
		keywords = node.getKeywords();
		assertFalse( keywords.hasNext() );

		node = parse( writer.toString(), 12); 
		assertNotNull( node );
		assertNotNull( node.getCompletionPrefix() );
		assertTrue( node.getCompletionScope() instanceof IASTClassSpecifier );
		assertEquals( node.getCompletionPrefix(), "i"); //$NON-NLS-1$
		assertEquals( node.getCompletionKind(), IASTCompletionNode.CompletionKind.FIELD_TYPE );
		keywords = node.getKeywords(); 
		assertTrue( keywords.hasNext() );
		assertEquals( (String) keywords.next(), "inline"); //$NON-NLS-1$
		assertEquals( (String) keywords.next(), "int"); //$NON-NLS-1$
		assertFalse( keywords.hasNext() );
		
		node = parse( writer.toString(), 22); 
		assertNotNull( node );
		assertNotNull( node.getCompletionPrefix() );
		assertEquals( node.getCompletionScope(), ((Scope)callback.getCompilationUnit()).getScope() );
		assertEquals( node.getCompletionPrefix(), "AB"); //$NON-NLS-1$
		assertEquals( node.getCompletionKind(), IASTCompletionNode.CompletionKind.VARIABLE_TYPE );
		keywords = node.getKeywords(); 
		assertFalse( keywords.hasNext() );
	
		node = parse( writer.toString(), 6); 
		assertNotNull( node );
		assertNotNull( node.getCompletionPrefix() );
		assertEquals( node.getCompletionScope(), ((Scope)callback.getCompilationUnit()).getScope() );
		assertEquals( node.getCompletionPrefix(), ""); //$NON-NLS-1$
		assertEquals( node.getCompletionKind(), IASTCompletionNode.CompletionKind.CLASS_REFERENCE );
		keywords = node.getKeywords(); 
		assertFalse( keywords.hasNext() );
	}
	
	public void testCompletionLookup_Unqualified() throws Exception
	{
		StringWriter writer = new StringWriter();
		writer.write( "int aVar; " ); //$NON-NLS-1$
		writer.write( "void foo( ) { " ); //$NON-NLS-1$
		writer.write( "   int anotherVar; " ); //$NON-NLS-1$
		writer.write( "   a " ); //$NON-NLS-1$
		writer.write( "} " ); //$NON-NLS-1$
		
		String code = writer.toString();
		
		for( int i = 0; i < 2; ++i )
		{	
			int index = ( i == 0 ? code.indexOf( " a " ) + 2 : code.indexOf( " a ") + 1 ); //$NON-NLS-1$ //$NON-NLS-2$
			
			IASTCompletionNode node = parse( code, index );	
			assertNotNull( node );
			
			String prefix = node.getCompletionPrefix();
			assertNotNull( prefix );
			assertTrue( node.getCompletionScope() instanceof IASTFunction );
			assertEquals( prefix, i == 0 ? "a" :"" ); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals( node.getCompletionKind(), IASTCompletionNode.CompletionKind.SINGLE_NAME_REFERENCE );
			
			IASTNode.LookupKind[] kinds = new IASTNode.LookupKind[1];
			kinds[0] = IASTNode.LookupKind.ALL; 
			ILookupResult result = node.getCompletionScope().lookup( prefix, kinds, node.getCompletionContext() );
			assertEquals( result.getPrefix(), prefix );
			
			Iterator iter = result.getNodes();
			
			IASTVariable anotherVar = (IASTVariable) iter.next();
			
			IASTVariable aVar = (IASTVariable) iter.next();
			
			if( i != 0 )
			{
				IASTFunction foo = (IASTFunction) iter.next();
				assertEquals( foo.getName(), "foo"); //$NON-NLS-1$
			}
					
			assertFalse( iter.hasNext() );
			assertEquals( anotherVar.getName(), "anotherVar" ); //$NON-NLS-1$
			assertEquals( aVar.getName(), "aVar" ); //$NON-NLS-1$
		}
	}
	
	public void testCompletionLookup_Qualified() throws Exception
	{
		StringWriter writer = new StringWriter();
		writer.write( "int aVar; " ); //$NON-NLS-1$
		writer.write( "struct D{ " ); //$NON-NLS-1$
		writer.write( "   int aField; " ); //$NON-NLS-1$
		writer.write( "   void aMethod(); " ); //$NON-NLS-1$
		writer.write( "}; " ); //$NON-NLS-1$
		writer.write( "void foo(){" ); //$NON-NLS-1$
		writer.write( "   D d; " ); //$NON-NLS-1$
		writer.write( "   d.a " ); //$NON-NLS-1$
		writer.write( "}\n" ); //$NON-NLS-1$
		
		String code = writer.toString();
		int index = code.indexOf( "d.a" ); //$NON-NLS-1$
		
		IASTCompletionNode node = parse( code, index + 3 );				
		assertNotNull( node );
		
		String prefix = node.getCompletionPrefix();
		assertNotNull( prefix );
		assertEquals( prefix, "a" ); //$NON-NLS-1$
		
		assertTrue( node.getCompletionScope() instanceof IASTFunction );
		assertEquals( node.getCompletionKind(), IASTCompletionNode.CompletionKind.MEMBER_REFERENCE );
		assertNotNull( node.getCompletionContext() );
		assertTrue( node.getCompletionContext() instanceof IASTVariable );
		
		IASTNode.LookupKind[] kinds = new IASTNode.LookupKind[1];
		kinds[0] = IASTNode.LookupKind.ALL; 
		ILookupResult result = node.getCompletionScope().lookup( prefix, kinds, node.getCompletionContext() );
		assertEquals( result.getPrefix(), prefix );
		
		Iterator iter = result.getNodes();
		
		IASTField aField = (IASTField) iter.next();
		IASTMethod aMethod = (IASTMethod) iter.next();
		
		assertFalse( iter.hasNext() );
		
		assertEquals( aMethod.getName(), "aMethod" ); //$NON-NLS-1$
		assertEquals( aField.getName(), "aField" ); //$NON-NLS-1$
	}
	
	public void testMemberCompletion_Arrow() throws Exception
	{
		StringWriter writer = new StringWriter();
		writer.write( "class A {" ); //$NON-NLS-1$
		writer.write( "   public:   void aPublicBaseMethod();" ); //$NON-NLS-1$
		writer.write( "   private:  void aPrivateBaseMethod();" ); //$NON-NLS-1$
		writer.write( "};" ); //$NON-NLS-1$
		writer.write( "class B : public A {" ); //$NON-NLS-1$
		writer.write( "   public:   void aMethod();" ); //$NON-NLS-1$
		writer.write( "};" );		 //$NON-NLS-1$
		writer.write( "void foo(){" );		 //$NON-NLS-1$
		writer.write( "   B * b = new B();" );		 //$NON-NLS-1$
		writer.write( "   b-> \n" ); //$NON-NLS-1$
		
		String code = writer.toString();
		int index = code.indexOf( "b->" ); //$NON-NLS-1$
		
		IASTCompletionNode node = parse( code, index + 3 );
		assertNotNull(node);
		assertEquals( node.getCompletionPrefix(), "" ); //$NON-NLS-1$
		
		assertEquals(node.getCompletionKind(), IASTCompletionNode.CompletionKind.MEMBER_REFERENCE);
		assertTrue(node.getCompletionScope() instanceof IASTFunction );
		assertEquals( ((IASTFunction)node.getCompletionScope()).getName(), "foo" );  //$NON-NLS-1$
		assertTrue(node.getCompletionContext() instanceof IASTVariable );
		assertEquals( ((IASTVariable)node.getCompletionContext()).getName(), "b" ); //$NON-NLS-1$
	}
	
	public void testMemberCompletion_Dot() throws Exception
	{
		StringWriter writer = new StringWriter();
		writer.write( "class A {" ); //$NON-NLS-1$
		writer.write( "   public:   void aPublicBaseMethod();" ); //$NON-NLS-1$
		writer.write( "   private:  void aPrivateBaseMethod();" ); //$NON-NLS-1$
		writer.write( "};" ); //$NON-NLS-1$
		writer.write( "class B : public A {" ); //$NON-NLS-1$
		writer.write( "   public:   void aMethod();" ); //$NON-NLS-1$
		writer.write( "};" );		 //$NON-NLS-1$
		writer.write( "void foo(){" );		 //$NON-NLS-1$
		writer.write( "   B b;" );		 //$NON-NLS-1$
		writer.write( "   b. \n" ); //$NON-NLS-1$
		
		String code = writer.toString();
		int index = code.indexOf( "b." ); //$NON-NLS-1$
		
		IASTCompletionNode node = parse( code, index + 2 );
		assertNotNull(node);
		assertEquals( node.getCompletionPrefix(), "" ); //$NON-NLS-1$
		
		assertEquals(node.getCompletionKind(), IASTCompletionNode.CompletionKind.MEMBER_REFERENCE);
		assertTrue(node.getCompletionScope() instanceof IASTFunction );
		assertEquals( ((IASTFunction)node.getCompletionScope()).getName(), "foo" );  //$NON-NLS-1$
		assertTrue(node.getCompletionContext() instanceof IASTVariable );
		assertEquals( ((IASTVariable)node.getCompletionContext()).getName(), "b" ); //$NON-NLS-1$
	}
	
	
	public void testCompletionLookup_Pointer() throws Exception{
		StringWriter writer = new StringWriter();
		writer.write( "class A {" ); //$NON-NLS-1$
		writer.write( "   public:   void aPublicBaseMethod();" ); //$NON-NLS-1$
		writer.write( "   private:  void aPrivateBaseMethod();" ); //$NON-NLS-1$
		writer.write( "};" ); //$NON-NLS-1$
		writer.write( "class B : public A {" ); //$NON-NLS-1$
		writer.write( "   public:   void aMethod();" ); //$NON-NLS-1$
		writer.write( "};" );		 //$NON-NLS-1$
		writer.write( "void foo(){" );		 //$NON-NLS-1$
		writer.write( "   B * b = new B();" );		 //$NON-NLS-1$
		writer.write( "   b->a \n" ); //$NON-NLS-1$
		
		for( int i = 0; i < 2; ++i )
		{	
			String code = writer.toString();
			
			int index;
			
			index = (i == 0 )? (code.indexOf( "b->a" )+4) :(code.indexOf( "b->") + 3); //$NON-NLS-1$ //$NON-NLS-2$
			
			IASTCompletionNode node = parse( code, index);
			
			assertNotNull( node );
			String prefix = node.getCompletionPrefix();
			
			assertEquals( prefix, ( i == 0 ) ? "a" :""); //$NON-NLS-1$ //$NON-NLS-2$
			
			assertTrue( node.getCompletionScope() instanceof IASTFunction );
			assertEquals( node.getCompletionKind(),  IASTCompletionNode.CompletionKind.MEMBER_REFERENCE );
			assertNotNull( node.getCompletionContext() );
			assertTrue( node.getCompletionContext() instanceof IASTVariable );
			
			IASTNode.LookupKind[] kinds = new IASTNode.LookupKind[1];
			kinds[0] = IASTNode.LookupKind.METHODS; 
			ILookupResult result = node.getCompletionScope().lookup( prefix, kinds, node.getCompletionContext() );
			assertEquals( result.getPrefix(), prefix );
			
			Iterator iter = result.getNodes();
			IASTMethod method = (IASTMethod) iter.next();
			IASTMethod baseMethod = (IASTMethod) iter.next();
			
			assertFalse( iter.hasNext() );
			
			assertEquals( method.getName(), "aMethod" ); //$NON-NLS-1$
			assertEquals( baseMethod.getName(), "aPublicBaseMethod" );		 //$NON-NLS-1$
		}
	}
	
	public void testCompletionLookup_FriendClass_1() throws Exception{
		StringWriter writer = new StringWriter();
		writer.write( "class A {" ); //$NON-NLS-1$
		writer.write( "   private:  void aPrivateMethod();" ); //$NON-NLS-1$
		writer.write( "   friend class C;" ); //$NON-NLS-1$
		writer.write( "};" ); //$NON-NLS-1$
		
		writer.write( "class C {" ); //$NON-NLS-1$
		writer.write( "   void foo();" ); //$NON-NLS-1$
		writer.write( "};" );	 //$NON-NLS-1$
		
		writer.write( "void C::foo(){" );		 //$NON-NLS-1$
		writer.write( "   A a;" );		 //$NON-NLS-1$
		writer.write( "   a.a \n" ); //$NON-NLS-1$
		
		String code = writer.toString();
		int index = code.indexOf( "a.a" ); //$NON-NLS-1$
		
		IASTCompletionNode node = parse( code, index + 3 );
		
		assertNotNull( node );
		
		String prefix = node.getCompletionPrefix();
		assertEquals( prefix, "a" ); //$NON-NLS-1$
		
		assertTrue( node.getCompletionScope() instanceof IASTFunction );
		assertEquals( node.getCompletionKind(),  IASTCompletionNode.CompletionKind.MEMBER_REFERENCE );
		assertNotNull( node.getCompletionContext() );
		assertTrue( node.getCompletionContext() instanceof IASTVariable );
		
		ILookupResult result = node.getCompletionScope().lookup( prefix, new IASTNode.LookupKind [] { IASTNode.LookupKind.METHODS }, node.getCompletionContext() );
		assertEquals( result.getPrefix(), prefix );
		
		Iterator iter = result.getNodes();
		assertTrue( iter.hasNext() );
		
		IASTMethod method = (IASTMethod) iter.next();
		
		assertFalse( iter.hasNext() );
		
		assertEquals( method.getName(), "aPrivateMethod" ); //$NON-NLS-1$
	}
	
	public void testCompletionLookup_FriendClass_2() throws Exception{
		StringWriter writer = new StringWriter();
		writer.write( "class C {" ); //$NON-NLS-1$
		writer.write( "   void foo();" ); //$NON-NLS-1$
		writer.write( "};" );		 //$NON-NLS-1$
		writer.write( "class A {" ); //$NON-NLS-1$
		writer.write( "   private:  void aPrivateMethod();" ); //$NON-NLS-1$
		writer.write( "   friend class C;" ); //$NON-NLS-1$
		writer.write( "};" ); //$NON-NLS-1$

		writer.write( "void C::foo(){" );		 //$NON-NLS-1$
		writer.write( "   A a;" );		 //$NON-NLS-1$
		writer.write( "   a.a \n" ); //$NON-NLS-1$
		
		String code = writer.toString();
		int index = code.indexOf( "a.a" ); //$NON-NLS-1$
		
		IASTCompletionNode node = parse( code, index + 3 );
		
		assertNotNull( node );
		
		String prefix = node.getCompletionPrefix();
		assertEquals( prefix, "a" ); //$NON-NLS-1$
		
		assertTrue( node.getCompletionScope() instanceof IASTFunction );
		assertEquals( node.getCompletionKind(),  IASTCompletionNode.CompletionKind.MEMBER_REFERENCE );
		assertNotNull( node.getCompletionContext() );
		assertTrue( node.getCompletionContext() instanceof IASTVariable );
		
		ILookupResult result = node.getCompletionScope().lookup( prefix, new IASTNode.LookupKind [] { IASTNode.LookupKind.METHODS }, node.getCompletionContext() );
		assertEquals( result.getPrefix(), prefix );
		
		Iterator iter = result.getNodes();
		assertTrue( iter.hasNext() );
		
		IASTMethod method = (IASTMethod) iter.next();
		
		assertFalse( iter.hasNext() );
		
		assertEquals( method.getName(), "aPrivateMethod" ); //$NON-NLS-1$
	}
	
	
	public void testCompletionLookup_ParametersAsLocalVariables() throws Exception{
		StringWriter writer = new StringWriter();
		writer.write( "int foo( int aParameter ){" ); //$NON-NLS-1$
		writer.write( "   int aLocal;" ); //$NON-NLS-1$
		writer.write( "   if( aLocal != 0 ){" );		 //$NON-NLS-1$
		writer.write( "      int aBlockLocal;" ); //$NON-NLS-1$
		writer.write( "      a \n" ); //$NON-NLS-1$
		
		String code = writer.toString();
		int index = code.indexOf( " a " ); //$NON-NLS-1$
		
		IASTCompletionNode node = parse( code, index + 2 );
		
		assertNotNull( node );
		
		String prefix = node.getCompletionPrefix();
		assertEquals( prefix, "a" ); //$NON-NLS-1$
		
		assertTrue( node.getCompletionScope() instanceof IASTCodeScope );
		assertEquals( node.getCompletionKind(),  IASTCompletionNode.CompletionKind.SINGLE_NAME_REFERENCE );
		assertNull( node.getCompletionContext() );
				
		ILookupResult result = node.getCompletionScope().lookup( prefix, new IASTNode.LookupKind [] { IASTNode.LookupKind.LOCAL_VARIABLES }, node.getCompletionContext() );
		assertEquals( result.getPrefix(), prefix );
		
		Iterator iter = result.getNodes();
				
		IASTVariable aBlockLocal = (IASTVariable) iter.next();
		IASTVariable aLocal = (IASTVariable) iter.next();
		IASTParameterDeclaration aParameter = (IASTParameterDeclaration) iter.next();
		
		assertFalse( iter.hasNext() );
		
		assertEquals( aBlockLocal.getName(), "aBlockLocal" ); //$NON-NLS-1$
		assertEquals( aLocal.getName(), "aLocal" ); //$NON-NLS-1$
		assertEquals( aParameter.getName(), "aParameter" ); //$NON-NLS-1$
	}
	
	public void testCompletionLookup_LookupKindTHIS() throws Exception{
		StringWriter writer = new StringWriter();
		writer.write( "int aGlobalVar;" ); //$NON-NLS-1$
		writer.write( "namespace NS { " ); //$NON-NLS-1$
		writer.write( "   int aNamespaceFunction(){}" ); //$NON-NLS-1$
		writer.write( "   class Base { " ); //$NON-NLS-1$
		writer.write( "      protected: int aBaseField;" ); //$NON-NLS-1$
		writer.write( "   };" ); //$NON-NLS-1$
		writer.write( "   class Derived : public Base {" ); //$NON-NLS-1$
		writer.write( "      int aMethod();" ); //$NON-NLS-1$
		writer.write( "   };" ); //$NON-NLS-1$
		writer.write( "}" ); //$NON-NLS-1$
		writer.write( "int NS::Derived::aMethod(){"); //$NON-NLS-1$
		writer.write( "   int aLocal;" ); //$NON-NLS-1$
		writer.write( "   a  "); //$NON-NLS-1$

		String code = writer.toString();
		int index = code.indexOf( " a " ); //$NON-NLS-1$
		
		IASTCompletionNode node = parse( code, index + 2 );
		
		assertNotNull( node );
		
		assertEquals( node.getCompletionPrefix(), "a" ); //$NON-NLS-1$
		assertTrue( node.getCompletionScope() instanceof IASTMethod );
		
		ILookupResult result = node.getCompletionScope().lookup( node.getCompletionPrefix(),
																new IASTNode.LookupKind[] { IASTNode.LookupKind.THIS },
																node.getCompletionContext() );
		
		assertEquals( result.getResultsSize(), 2 );
		
		Iterator iter = result.getNodes();
		IASTMethod method = (IASTMethod) iter.next();
		IASTField field = (IASTField) iter.next();
		assertFalse( iter.hasNext() );
		assertEquals( method.getName(), "aMethod" ); //$NON-NLS-1$
		assertEquals( field.getName(), "aBaseField" ); //$NON-NLS-1$
		
		result = node.getCompletionScope().lookup( node.getCompletionPrefix(),
												   new IASTNode.LookupKind[] { IASTNode.LookupKind.THIS, IASTNode.LookupKind.METHODS },
												   node.getCompletionContext() );
		
		assertEquals( result.getResultsSize(), 1 );
		iter = result.getNodes();
		method = (IASTMethod) iter.next();
		assertFalse( iter.hasNext() );
		assertEquals( method.getName(), "aMethod" ); //$NON-NLS-1$
	}
	
	public void testCompletionInConstructor() throws Exception
	{
		Writer writer = new StringWriter();
		writer.write("class SimpleTest{"); //$NON-NLS-1$
		writer.write("	public:"); //$NON-NLS-1$
		writer.write("SimpleTest();"); //$NON-NLS-1$
		writer.write("~SimpleTest();"); //$NON-NLS-1$
		writer.write("int a, b, c, aa, bb, cc, abc;"); //$NON-NLS-1$
		writer.write("};"); //$NON-NLS-1$
		writer.write("SimpleTest::~SimpleTest()"); //$NON-NLS-1$
		writer.write("{}"); //$NON-NLS-1$
		writer.write("SimpleTest::SimpleTest()"); //$NON-NLS-1$
		writer.write("{"); //$NON-NLS-1$
		writer.write("/**/a"); //$NON-NLS-1$
		writer.write("}"); //$NON-NLS-1$

		IASTCompletionNode node = parse( writer.toString(), writer.toString().indexOf("/**/a") + 5 ); //$NON-NLS-1$
		assertNotNull(node);
		assertEquals(node.getCompletionPrefix(), "a"); //$NON-NLS-1$
		assertTrue(node.getCompletionScope() instanceof IASTMethod);
		IASTMethod inquestion = (IASTMethod)node.getCompletionScope();
		assertEquals( inquestion.getName(), "SimpleTest"); //$NON-NLS-1$
		assertTrue(inquestion.isConstructor());
		
		assertEquals(node.getCompletionKind(), IASTCompletionNode.CompletionKind.SINGLE_NAME_REFERENCE );
		assertNull(node.getCompletionContext());
		LookupKind[] kinds = new LookupKind[ 1 ];
		kinds[0] = LookupKind.FIELDS;
		
		ILookupResult result = inquestion.lookup( "a", kinds, null ); //$NON-NLS-1$
		assertEquals(result.getResultsSize(), 3 );
	}
	
	public void testCompletionInDestructor() throws Exception
	{
		Writer writer = new StringWriter();
		writer.write("class SimpleTest{"); //$NON-NLS-1$
		writer.write("	public:"); //$NON-NLS-1$
		writer.write("SimpleTest();"); //$NON-NLS-1$
		writer.write("~SimpleTest();"); //$NON-NLS-1$
		writer.write("int a, b, c, aa, bb, cc, abc;"); //$NON-NLS-1$
		writer.write("};"); //$NON-NLS-1$
		writer.write("SimpleTest::SimpleTest()"); //$NON-NLS-1$
		writer.write("{}"); //$NON-NLS-1$
		writer.write("SimpleTest::~SimpleTest()"); //$NON-NLS-1$
		writer.write("{"); //$NON-NLS-1$
		writer.write("/**/a"); //$NON-NLS-1$
		writer.write("}"); //$NON-NLS-1$

		IASTCompletionNode node = parse( writer.toString(), writer.toString().indexOf("/**/a") + 5 ); //$NON-NLS-1$
		assertNotNull(node);
		assertEquals(node.getCompletionPrefix(), "a"); //$NON-NLS-1$
		assertTrue(node.getCompletionScope() instanceof IASTMethod);
		IASTMethod inquestion = (IASTMethod)node.getCompletionScope();
		assertEquals( inquestion.getName(), "~SimpleTest"); //$NON-NLS-1$
		assertTrue(inquestion.isDestructor());
		
		assertEquals(node.getCompletionKind(), IASTCompletionNode.CompletionKind.SINGLE_NAME_REFERENCE );
		assertNull(node.getCompletionContext());
		LookupKind[] kinds = new LookupKind[ 1 ];
		kinds[0] = LookupKind.FIELDS;
		
		ILookupResult result = inquestion.lookup( "a", kinds, null ); //$NON-NLS-1$
		assertEquals(result.getResultsSize(), 3 );
	}
	
	public void testBug48307_FriendFunction_1() throws Exception {
		StringWriter writer = new StringWriter();
		writer.write( "class A{ public : void foo(); }; " ); //$NON-NLS-1$
		writer.write( "class B{ "); //$NON-NLS-1$
		writer.write( "   private : int aPrivate;" ); //$NON-NLS-1$
		writer.write( "   friend void A::foo(); "); //$NON-NLS-1$
		writer.write( "};" ); //$NON-NLS-1$
		writer.write( "void A::foo(){" ); //$NON-NLS-1$
		writer.write( "   B b;"); //$NON-NLS-1$
		writer.write( "   b.aP" ); //$NON-NLS-1$
		
		String code = writer.toString();
		int index = code.indexOf( "b.aP" ); //$NON-NLS-1$
		IASTCompletionNode node = parse( code, index + 4  );
		
		ILookupResult result = node.getCompletionScope().lookup( node.getCompletionPrefix(), 
				new IASTNode.LookupKind[] { IASTNode.LookupKind.ALL }, 
				node.getCompletionContext() );

		assertEquals( result.getResultsSize(), 1 );
		IASTField field = (IASTField) result.getNodes().next();
		assertEquals( field.getName(), "aPrivate" ); //$NON-NLS-1$
	}

	public void testBug48307_FriendFunction_2() throws Exception {
		StringWriter writer = new StringWriter();
		writer.write( "void global();" ); //$NON-NLS-1$
		writer.write( "class B{ "); //$NON-NLS-1$
		writer.write( "   private : int aPrivate;" ); //$NON-NLS-1$
		writer.write( "   friend void global(); "); //$NON-NLS-1$
		writer.write( "};" ); //$NON-NLS-1$
		writer.write( "void global(){" ); //$NON-NLS-1$
		writer.write( "   B b;"); //$NON-NLS-1$
		writer.write( "   b.aP" ); //$NON-NLS-1$
		
		String code = writer.toString();
		int index = code.indexOf( "b.aP" ); //$NON-NLS-1$
		IASTCompletionNode node = parse( code, index + 4  );
		
		ILookupResult result = node.getCompletionScope().lookup( node.getCompletionPrefix(), 
				new IASTNode.LookupKind[] { IASTNode.LookupKind.ALL }, 
				node.getCompletionContext() );

		assertEquals( result.getResultsSize(), 1 );
		IASTField field = (IASTField) result.getNodes().next();
		assertEquals( field.getName(), "aPrivate" ); //$NON-NLS-1$
	}
	
	public void testBug51260() throws Exception{
		StringWriter writer = new StringWriter();
		writer.write( " class A { public: void a(); }; " ); //$NON-NLS-1$
		writer.write( " class B : public virtual A { public: void b(); };" ); //$NON-NLS-1$
		writer.write( " class C : public virtual A { public: void c(); };" ); //$NON-NLS-1$
		writer.write( " class D : public B, C { public: void d(); };" ); //$NON-NLS-1$
		
		writer.write( " void A::a(){} "); //$NON-NLS-1$
		writer.write( " void B::b(){} "); //$NON-NLS-1$
		writer.write( " void C::c(){} "); //$NON-NLS-1$
		writer.write( " void D::d(){ SP }" ); //$NON-NLS-1$
		
		String code = writer.toString();
		int index = code.indexOf( "SP" ); //$NON-NLS-1$
		IASTCompletionNode node = parse( code, index );
		
		ILookupResult result = node.getCompletionScope().lookup( node.getCompletionPrefix(), 
		                                                         new IASTNode.LookupKind[]{ IASTNode.LookupKind.THIS },
																 node.getCompletionContext() );
		
		assertEquals( result.getResultsSize(), 4 );
		
		Iterator iter = result.getNodes();
		IASTMethod d = (IASTMethod) iter.next();
		IASTMethod b = (IASTMethod) iter.next();
		IASTMethod a = (IASTMethod) iter.next();
		IASTMethod c = (IASTMethod) iter.next();
		
		assertFalse( iter.hasNext() );
		
		assertEquals( a.getName(), "a" ); //$NON-NLS-1$
		assertEquals( b.getName(), "b" ); //$NON-NLS-1$
		assertEquals( c.getName(), "c" ); //$NON-NLS-1$
		assertEquals( d.getName(), "d" ); //$NON-NLS-1$
		
	}
	
	public void testBug52948() throws Exception{
		StringWriter writer = new StringWriter();
		writer.write( "typedef int Int; "); //$NON-NLS-1$
		writer.write( "InSP" ); //$NON-NLS-1$
		
		String code = writer.toString();
		int index = code.indexOf( "SP" ); //$NON-NLS-1$
		
		IASTCompletionNode node = parse( code, index );
		ILookupResult result = node.getCompletionScope().lookup( node.getCompletionPrefix(), 
                                                                 new IASTNode.LookupKind[]{ IASTNode.LookupKind.TYPEDEFS },
				                                                 node.getCompletionContext() );
		
		assertEquals( result.getResultsSize(), 1 );
		
		Iterator iter = result.getNodes();
		IASTTypedefDeclaration typeDef = (IASTTypedefDeclaration) iter.next();
		
		assertEquals( typeDef.getName(), "Int" ); //$NON-NLS-1$
		assertFalse( iter.hasNext() );
	}
	
	
	public void testCompletionInFunctionBodyFullyQualified() throws Exception
	{
		StringWriter writer = new StringWriter();
		writer.write( "int aInteger = 5;\n"); //$NON-NLS-1$
		writer.write( "namespace NMS { \n"); //$NON-NLS-1$
		writer.write( " int foo() { \n"); //$NON-NLS-1$
		writer.write( "::A "); //$NON-NLS-1$
		writer.write( "}\n}\n"); //$NON-NLS-1$
		String code = writer.toString();
		
		for( int i = 0; i < 2; ++i )
		{
			String stringToCompleteAfter = ( i == 0 ) ? "::" : "::A"; //$NON-NLS-1$ //$NON-NLS-2$
			IASTCompletionNode node = parse( code, code.indexOf( stringToCompleteAfter) + stringToCompleteAfter.length() );
			
			validateCompletionNode(node, ( i == 0 ? "" : "A"), IASTCompletionNode.CompletionKind.SINGLE_NAME_REFERENCE, getCompilationUnit(), false ); //$NON-NLS-1$ //$NON-NLS-2$
			
			ILookupResult result = node.getCompletionScope().lookup( node.getCompletionPrefix(), 
	                new IASTNode.LookupKind[]{ IASTNode.LookupKind.ALL },
	                node.getCompletionContext() );
	
			Set results = new HashSet();
			results.add( "aInteger"); //$NON-NLS-1$
			if( i == 0 )
				results.add( "NMS"); //$NON-NLS-1$
			validateLookupResult(result, results );
		}
	}

	public void testCompletionInFunctionBodyQualifiedName() throws Exception
	{
		StringWriter writer = new StringWriter();
		writer.write( "namespace ABC {\n"); //$NON-NLS-1$
		writer.write( "  struct DEF { int x; }; \n" ); //$NON-NLS-1$
		writer.write( "  struct GHI { float y;};\n"); //$NON-NLS-1$
		writer.write( "}\n"); //$NON-NLS-1$
		writer.write( "int main() { ABC::D }\n"); //$NON-NLS-1$
		String code = writer.toString();
		
		for( int j = 0; j< 2; ++j )
		{
			String stringToCompleteAfter = (j == 0 ) ? "::" : "::D"; //$NON-NLS-1$ //$NON-NLS-2$
			IASTCompletionNode node = parse( code, code.indexOf( stringToCompleteAfter) + stringToCompleteAfter.length() );
			
			IASTNamespaceDefinition namespaceDefinition = null;
			Iterator i = callback.getCompilationUnit().getDeclarations();
			while( i.hasNext() )
			{
				IASTDeclaration d = (IASTDeclaration) i.next();
				if( d instanceof IASTNamespaceDefinition ) 
					if( ((IASTNamespaceDefinition)d).getName().equals( "ABC") ) //$NON-NLS-1$
					{
						namespaceDefinition = (IASTNamespaceDefinition) d;
						break;
					}
			}
			assertNotNull( namespaceDefinition );
			validateCompletionNode( node, 
					( j == 0 ) ? "" : "D",  //$NON-NLS-1$ //$NON-NLS-2$
					IASTCompletionNode.CompletionKind.SINGLE_NAME_REFERENCE, namespaceDefinition, false ); 
	
			ILookupResult result = node.getCompletionScope().lookup( node.getCompletionPrefix(), 
	                new IASTNode.LookupKind[]{ IASTNode.LookupKind.ALL },
	                node.getCompletionContext() );
			
			Set results = new HashSet();
			results.add( "DEF"); //$NON-NLS-1$
			if( j == 0 )
				results.add( "GHI"); //$NON-NLS-1$
			validateLookupResult(result, results );
		}

	}
	
	public void testCompletionWithTemplateInstanceAsParent() throws Exception
	{
		StringWriter writer = new StringWriter();
		writer.write( "template < class T > class A { public : int a_member; }; "); //$NON-NLS-1$
		writer.write( "template < class T > class B : public A< T > { public : int b_member; }; "); //$NON-NLS-1$
		writer.write( "void f() { "); //$NON-NLS-1$
		writer.write( "   B< int > b; "); //$NON-NLS-1$
		writer.write( "   b.SP "); //$NON-NLS-1$
		
		String code = writer.toString();
		IASTCompletionNode node = parse( code, code.indexOf( "SP" ) ); //$NON-NLS-1$
		
		ILookupResult result = node.getCompletionScope().lookup( "",  //$NON-NLS-1$
				                                                 new IASTNode.LookupKind[]{ IASTNode.LookupKind.ALL }, 
																 node.getCompletionContext() );
		assertEquals( result.getResultsSize(), 2 );
		
		Iterator i = result.getNodes();
		
		assertTrue( i.next() instanceof IASTField );
		assertTrue( i.next() instanceof IASTField );
		assertFalse( i.hasNext() );
	}
	
	public void testBug58178() throws Exception
	{
		Writer writer = new StringWriter();
		writer.write( "#define GL_T 0x2001\n"); //$NON-NLS-1$
		writer.write( "#define GL_TRUE 0x1\n"); //$NON-NLS-1$
		writer.write( "typedef unsigned char   GLboolean;\n"); //$NON-NLS-1$
		writer.write( "static GLboolean should_rotate = GL_T"); //$NON-NLS-1$
		String code = writer.toString();
		final String where = "= GL_T"; //$NON-NLS-1$
		IASTCompletionNode node = parse( code, code.indexOf( where ) + where.length() );
		assertEquals( node.getCompletionPrefix(), "GL_T"); //$NON-NLS-1$
	}

	public void testBug52253() throws Exception
	{
		Writer writer = new StringWriter();
		writer.write( "class CMyClass {public:\n void doorBell(){ return; }};"); //$NON-NLS-1$
		writer.write( "int	main(int argc, char **argv) {CMyClass mc; mc.do }"); //$NON-NLS-1$
		String code = writer.toString();
		final String where = "mc.do"; //$NON-NLS-1$
		IASTCompletionNode node = parse( code, code.indexOf( where) + where.length() );
		assertEquals( node.getCompletionPrefix(), "do"); //$NON-NLS-1$
		assertEquals( node.getCompletionKind(), CompletionKind.MEMBER_REFERENCE );
		ILookupResult result = node.getCompletionScope().lookup( node.getCompletionPrefix(), 
                new IASTNode.LookupKind[]{ IASTNode.LookupKind.ALL }, 
				 node.getCompletionContext() );
		assertEquals( result.getResultsSize(), 1 );
		Iterator i = result.getNodes();
		IASTMethod doorBell = (IASTMethod) i.next();
		assertFalse( i.hasNext() );
		assertEquals( doorBell.getName(), "doorBell"); //$NON-NLS-1$
		
	}
	
	public void testBug58492() throws Exception
	{
		Writer writer = new StringWriter();
		writer.write("struct Cube {                       "); //$NON-NLS-1$
		writer.write("   int nLen;                        "); //$NON-NLS-1$
		writer.write("   int nWidth;                      "); //$NON-NLS-1$
		writer.write("   int nHeight;                     "); //$NON-NLS-1$
		writer.write("};                                  "); //$NON-NLS-1$
		writer.write("int volume( struct Cube * pCube ) { "); //$NON-NLS-1$
		writer.write("   pCube->SP                        "); //$NON-NLS-1$

		String code = writer.toString();
		IASTCompletionNode node = parse( code, code.indexOf("SP"), ParserLanguage.C ); //$NON-NLS-1$
		
		ILookupResult result = node.getCompletionScope().lookup( node.getCompletionPrefix(),
				                                                 new IASTNode.LookupKind[] {IASTNode.LookupKind.ALL },
																 node.getCompletionContext() );
		assertEquals( result.getResultsSize(), 3 );
		Iterator i = result.getNodes();
		assertTrue( i.next() instanceof IASTField );
		assertTrue( i.next() instanceof IASTField );
		assertTrue( i.next() instanceof IASTField );
	}
	
	public void testCompletionOnExpression() throws Exception
	{
		Writer writer = new StringWriter();
		writer.write( "class ABC { public: void voidMethod(); };\n"); //$NON-NLS-1$
		writer.write( "ABC * someFunction(void) { return new ABC(); }\n"); //$NON-NLS-1$
		writer.write( "void testFunction( void ) { someFunction()->V  }\n" ); //$NON-NLS-1$
		String code = writer.toString();
		for( int i = 0; i < 2; ++i )
		{
			int index = code.indexOf( "V"); //$NON-NLS-1$
			if( i == 1 ) ++index;
			IASTCompletionNode node = parse( code, index );
			assertEquals( node.getCompletionPrefix(), (i == 0 )? "": "V"); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals( node.getCompletionKind(), CompletionKind.MEMBER_REFERENCE );
			assertTrue( node.getCompletionContext() instanceof IASTExpression );
		}
		
	}
	
	public void testCompletionInTypeDef() throws Exception{
		StringWriter writer = new StringWriter();
		writer.write( "struct A {  int name;  };  \n" ); //$NON-NLS-1$
		writer.write( "typedef struct A * PA;     \n" ); //$NON-NLS-1$
		writer.write( "int main() {               \n" ); //$NON-NLS-1$
		writer.write( "   PA a;                   \n" ); //$NON-NLS-1$
		writer.write( "   a->SP                   \n" ); //$NON-NLS-1$
		writer.write( "}                          \n" ); //$NON-NLS-1$
		
		String code = writer.toString();
		int index = code.indexOf( "SP" ); //$NON-NLS-1$
		
		IASTCompletionNode node = parse( code, index );
		ILookupResult result = node.getCompletionScope().lookup( node.getCompletionPrefix(), 
                                                                 new IASTNode.LookupKind[]{ IASTNode.LookupKind.ALL },
				                                                 node.getCompletionContext() );
		assertEquals( result.getResultsSize(), 1 );
		
		Iterator iter = result.getNodes();
		IASTField name = (IASTField) iter.next();
		
		assertEquals( name.getName(), "name" ); //$NON-NLS-1$
		assertFalse( iter.hasNext() );
	}
	
	public void testBug59134() throws Exception
	{
		String code = "int main(){ siz }"; //$NON-NLS-1$
		IASTCompletionNode node = parse( code, code.indexOf(" siz") ); //$NON-NLS-1$
		assertNotNull( node );
		Iterator keywords = node.getKeywords();
		boolean passed = false;
		while( keywords.hasNext() )
		{
			String keyword = (String) keywords.next();
			if( keyword.equals( "sizeof")) //$NON-NLS-1$
				passed = true;
		}
		assertTrue( passed );
		
	}
	
	public void testBug59893() throws Exception
	{
		StringWriter writer = new StringWriter();
		writer.write( "struct A {  	                 \n" ); //$NON-NLS-1$ 
		writer.write( "   void f1() const volatile;	 \n" ); //$NON-NLS-1$ 
		writer.write( "   void f2() const;  		 \n" ); //$NON-NLS-1$
		writer.write( "   void f3() volatile;        \n" ); //$NON-NLS-1$
		writer.write( "   void f4();                 \n" ); //$NON-NLS-1$
		writer.write( "};                            \n" ); //$NON-NLS-1$
		writer.write( "void main( const A& a1 )      \n" ); //$NON-NLS-1$
		writer.write( "{                             \n" ); //$NON-NLS-1$
		writer.write( "   const volatile A * a2;     \n" ); //$NON-NLS-1$
		writer.write( "   const A * a3;              \n" ); //$NON-NLS-1$
		writer.write( "   volatile A * a4;           \n" ); //$NON-NLS-1$
		writer.write( "   A * a5;                    \n" ); //$NON-NLS-1$
		
		String code = writer.toString();
		
		IASTCompletionNode node = parse( code + "a1. ", code.length() + 3 ); //$NON-NLS-1$
		
		assertNotNull( node );
		ILookupResult result = node.getCompletionScope().lookup( node.getCompletionPrefix(),
				                                                 new IASTNode.LookupKind[]{ IASTNode.LookupKind.ALL },
		                                                         node.getCompletionContext() );
		assertEquals( result.getResultsSize(), 2 );
		
		node = parse( code + "a2-> ", code.length() + 4 ); //$NON-NLS-1$
		assertNotNull( node );
		result = node.getCompletionScope().lookup( node.getCompletionPrefix(),
		                                           new IASTNode.LookupKind[]{ IASTNode.LookupKind.ALL },
		                                           node.getCompletionContext() );
		assertEquals( result.getResultsSize(), 1 );
		
		node = parse( code + "a3-> ", code.length() + 4 ); //$NON-NLS-1$
		assertNotNull( node );
		result = node.getCompletionScope().lookup( node.getCompletionPrefix(),
		                                           new IASTNode.LookupKind[]{ IASTNode.LookupKind.ALL },
		                                           node.getCompletionContext() );
		assertEquals( result.getResultsSize(), 2 );
		
		node = parse( code + "a4-> ", code.length() + 4 ); //$NON-NLS-1$
		assertNotNull( node );
		result = node.getCompletionScope().lookup( node.getCompletionPrefix(),
		                                           new IASTNode.LookupKind[]{ IASTNode.LookupKind.ALL },
		                                           node.getCompletionContext() );
		assertEquals( result.getResultsSize(), 2 );
		
		node = parse( code + "a5-> ", code.length() + 4 ); //$NON-NLS-1$
		assertNotNull( node );
		result = node.getCompletionScope().lookup( node.getCompletionPrefix(),
		                                           new IASTNode.LookupKind[]{ IASTNode.LookupKind.ALL },
		                                           node.getCompletionContext() );
		assertEquals( result.getResultsSize(), 4 );
	}
	
	public void testBug59893_Expression() throws Exception
	{
		StringWriter writer = new StringWriter();
		writer.write( "struct A {  	                 \n" ); //$NON-NLS-1$ 
		writer.write( "   void f2() const;  		 \n" ); //$NON-NLS-1$
		writer.write( "   void f4();                 \n" ); //$NON-NLS-1$
		writer.write( "};                            \n" ); //$NON-NLS-1$
		writer.write( "const A * foo(){}             \n" ); //$NON-NLS-1$
		writer.write( "void main( )                  \n" ); //$NON-NLS-1$
		writer.write( "{                             \n" ); //$NON-NLS-1$
		writer.write( "   foo()->SP                  \n" ); //$NON-NLS-1$
		
		String code = writer.toString();
		int index = code.indexOf( "SP" ); //$NON-NLS-1$
		
		IASTCompletionNode node = parse( code, index );
		ILookupResult result = node.getCompletionScope().lookup( node.getCompletionPrefix(), 
                                                                 new IASTNode.LookupKind[]{ IASTNode.LookupKind.ALL },
				                                                 node.getCompletionContext() );
		assertEquals( result.getResultsSize(), 1 );
	}
	
	public void testParameterListFunctionReference() throws Exception
	{
		Writer writer = new StringWriter();
		writer.write( "int foo( int firstParam, int secondParam );\n"); //$NON-NLS-1$
		writer.write( "void main() { \n"); //$NON-NLS-1$
		writer.write( "  int abc;\n"); //$NON-NLS-1$
		writer.write( "  int x;\n" ); //$NON-NLS-1$
		writer.write( "  foo( x,a"); //$NON-NLS-1$
		String code = writer.toString();
		for( int i = 0; i < 2; ++i )
		{
			int index = code.indexOf( "x,a") + 2; //$NON-NLS-1$
			if( i == 1 ) index++;
			IASTCompletionNode node = parse( code, index );
			validateCompletionNode(node, (( i == 0 ) ? "" : "a" ), CompletionKind.FUNCTION_REFERENCE, null, true ); //$NON-NLS-1$ //$NON-NLS-2$
			assertNotNull( node.getFunctionParameters() );
			ILookupResult result = node.getCompletionScope().lookup( node.getCompletionPrefix(), 
                    new IASTNode.LookupKind[]{ IASTNode.LookupKind.LOCAL_VARIABLES },
                    node.getCompletionContext() );
			assertNotNull(result);
			assertEquals( result.getResultsSize(), ( i == 0 ) ? 2 : 1 );
		}
	}
	
	public void testParameterListConstructorReference() throws Exception
	{
		Writer writer = new StringWriter();
		writer.write( "class A { \n"); //$NON-NLS-1$
		writer.write( "public:\n"); //$NON-NLS-1$
		writer.write( "  A( int first, int second );\n"); //$NON-NLS-1$
		writer.write( "};\n" ); //$NON-NLS-1$
		writer.write( "void main() { \n"); //$NON-NLS-1$
		writer.write( "  int four, x;"); //$NON-NLS-1$
		writer.write( "  A * a = new A( x,f "); //$NON-NLS-1$
		String code = writer.toString();
		for( int i = 0; i < 2; ++i )
		{
			int index = code.indexOf( "x,f") + 2; //$NON-NLS-1$
			if( i == 1 ) index++;
			IASTCompletionNode node = parse( code, index );
			validateCompletionNode(node, (( i == 0 ) ? "" : "f" ), CompletionKind.CONSTRUCTOR_REFERENCE, null, true ); //$NON-NLS-1$ //$NON-NLS-2$
			assertNotNull( node.getFunctionParameters() );
			ILookupResult result = node.getCompletionScope().lookup( node.getCompletionPrefix(), 
                    new IASTNode.LookupKind[]{ IASTNode.LookupKind.LOCAL_VARIABLES },
                    node.getCompletionContext() );
			assertNotNull(result);
			assertEquals( result.getResultsSize(), ( i == 0 ) ? 2 : 1 );
		}
	}

}
