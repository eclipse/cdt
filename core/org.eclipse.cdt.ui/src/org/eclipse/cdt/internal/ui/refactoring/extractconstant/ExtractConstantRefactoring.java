/*******************************************************************************
 * Copyright (c) 2008, 2016 Institute for Software, HSR Hochschule fuer Technik  
 * Rapperswil, University of applied sciences and others
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html  
 *  
 * Contributors: 
 *    Institute for Software - initial API and implementation
 *    Sergey Prigogin (Google)
 *    Thomas Corbat (IFS)
 *******************************************************************************/
package org.eclipse.cdt.internal.ui.refactoring.extractconstant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.cdt.core.dom.ast.ASTNodeFactoryFactory;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTMacroExpansionLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.INodeFactory;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPNodeFactory;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.dom.rewrite.DeclarationGenerator;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.ui.PreferenceConstants;

import org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.CPPVisitor;

import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoringDescriptor;
import org.eclipse.cdt.internal.ui.refactoring.ClassMemberInserter;
import org.eclipse.cdt.internal.ui.refactoring.MethodContext;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.internal.ui.refactoring.utils.IdentifierHelper;
import org.eclipse.cdt.internal.ui.refactoring.utils.NodeHelper;
import org.eclipse.cdt.internal.ui.refactoring.utils.SelectionHelper;
import org.eclipse.cdt.internal.ui.util.NameComposer;

/**
 * The main class of the Extract Constant refactoring.
 * 
 * @author Mirko Stocker
 */
public class ExtractConstantRefactoring extends CRefactoring {
	private final class SelectedExpressionFinderVisitor extends ASTVisitor {
		{
			shouldVisitExpressions = true;
		}

		private IASTExpression selectedExpression;

		@Override
		public int visit(IASTExpression expression) {
			if (SelectionHelper.nodeMatchesSelection(expression, selectedRegion)) {
				selectedExpression = expression;
				return PROCESS_ABORT;
			} else if (expression instanceof IASTLiteralExpression && SelectionHelper.isSelectionInsideNode(expression, selectedRegion)) {
				selectedExpression = expression;
				return PROCESS_ABORT;
			}
			return super.visit(expression);
		}
	}

	private static final String PREFIX_FOR_NAME_WITH_LEADING_DIGIT = "_"; //$NON-NLS-1$

	public static final String ID =
			"org.eclipse.cdt.ui.refactoring.extractconstant.ExtractConstantRefactoring"; //$NON-NLS-1$

	private IASTExpression target;
	private final ExtractConstantInfo info;

	public ExtractConstantRefactoring(ICElement element, ISelection selection, ICProject project) {
		super(element, selection, project);
		this.info = new ExtractConstantInfo();
		name = Messages.ExtractConstantRefactoring_ExtractConst;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		SubMonitor sm = SubMonitor.convert(pm, 12);

		try {
			RefactoringStatus status = super.checkInitialConditions(sm.newChild(8));
			if (status.hasError()) {
				return status;
			}

			if (selectedRegion == null) {
				status.addFatalError(Messages.ExtractConstantRefactoring_LiteralMustBeSelected);
				return status;
			}

			IASTExpression selectedExpression = findSelectedExpression(sm.newChild(1));
			if (selectedExpression == null) {
				status.addFatalError(Messages.ExtractConstantRefactoring_LiteralMustBeSelected);
				return status;
			}

			if (!isExtractableExpression(selectedExpression)) {
				status.addFatalError(Messages.ExtractConstantRefactoring_LiteralMustBeSelected);
			}

			Collection<IASTLiteralExpression> literalExpressionCollection = findAllLiterals(sm.newChild(1));
			if (literalExpressionCollection.isEmpty()) {
				status.addFatalError(Messages.ExtractConstantRefactoring_LiteralMustBeSelected); 
				return status;
			}

			target = selectedExpression;

			if (info.getName().isEmpty()) {
				info.setName(getDefaultName(target));
			}
			info.setMethodContext(NodeHelper.findMethodContext(target, refactoringContext, sm.newChild(1)));
			IScope containingScope = CPPVisitor.getContainingScope(target);
			IASTTranslationUnit ast = target.getTranslationUnit();
			info.setNameUsedChecker((String name) -> { 
				IBinding[] bindingsForName = containingScope.find(name, ast);
				return bindingsForName.length != 0;
			});
			return status;
		} finally {
			if (pm != null) {
				pm.done();
			}
		}
	}

	private IASTExpression findSelectedExpression(IProgressMonitor monitor)
			throws OperationCanceledException, CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 2);
		try {
			IASTTranslationUnit ast = getAST(tu, progress.newChild(1));
			SelectedExpressionFinderVisitor expressionFinder = new SelectedExpressionFinderVisitor();
			ast.accept(expressionFinder);
			return expressionFinder.selectedExpression;
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}

	private boolean isExtractableExpression(IASTExpression expression) {
		if (expression instanceof IASTLiteralExpression) {
			return true;
		}
		if (expression instanceof IASTUnaryExpression) {
			IASTUnaryExpression unaryExpression = (IASTUnaryExpression) expression;
			return isExtractableExpression(unaryExpression.getOperand());
		}
		if (expression instanceof IASTBinaryExpression) {
			IASTBinaryExpression binaryExpression = (IASTBinaryExpression) expression;
			return isExtractableExpression(binaryExpression.getOperand1()) && isExtractableExpression(binaryExpression.getOperand2());
		}
		return false;
	}

	private String getDefaultName(IASTExpression expression) {
		String nameString = expression.getRawSignature();
		NameComposer composer = createNameComposer();
		String composedName = composer.compose(nameString);
		if (IdentifierHelper.isLeadingADigit(composedName)) {
			composedName = PREFIX_FOR_NAME_WITH_LEADING_DIGIT + composedName;
		}
		return composedName;
	}

	private static NameComposer createNameComposer() {
		IPreferencesService preferences = Platform.getPreferencesService();
		int capitalization = preferences.getInt(CUIPlugin.PLUGIN_ID,
				PreferenceConstants.NAME_STYLE_CONSTANT_CAPITALIZATION,
				PreferenceConstants.NAME_STYLE_CAPITALIZATION_UPPER_CASE, null);
		String wordDelimiter = preferences.getString(CUIPlugin.PLUGIN_ID,
				PreferenceConstants.NAME_STYLE_CONSTANT_WORD_DELIMITER, "_", null); //$NON-NLS-1$
		String prefix = preferences.getString(CUIPlugin.PLUGIN_ID,
				PreferenceConstants.NAME_STYLE_CONSTANT_PREFIX, "", null); //$NON-NLS-1$
		String suffix = preferences.getString(CUIPlugin.PLUGIN_ID,
				PreferenceConstants.NAME_STYLE_CONSTANT_SUFFIX, "", null); //$NON-NLS-1$
		NameComposer composer = new NameComposer(capitalization, wordDelimiter, prefix, suffix);
		return composer;
	}

	private Collection<IASTLiteralExpression> findAllLiterals(IProgressMonitor monitor)
			throws OperationCanceledException, CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 2);
		try {
			final Collection<IASTLiteralExpression> result = new ArrayList<IASTLiteralExpression>();

			IASTTranslationUnit ast = getAST(tu, progress.newChild(1));
			ast.accept(new ASTVisitor() {
				{
					shouldVisitExpressions = true;
				}

				@Override
				public int visit(IASTExpression expression) {
					if (expression instanceof IASTLiteralExpression) {
						if (!(expression.getNodeLocations().length == 1 &&
								expression.getNodeLocations()[0] instanceof IASTMacroExpansionLocation)) {
							IASTLiteralExpression literal = (IASTLiteralExpression) expression;
							result.add(literal);
						}
					}
					return super.visit(expression);
				}
			});

			return result;
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}

	@Override
	protected void collectModifications(IProgressMonitor monitor, ModificationCollector collector)
			throws CoreException, OperationCanceledException{
		SubMonitor progress = SubMonitor.convert(monitor, 100);
		try {
			Collection<IASTExpression> expressionsToReplace = findExpressionsToExtract(progress.newChild(40));
			Collection<IASTExpression> expressionsToReplaceInSameContext = filterLiteralsInSameContext(expressionsToReplace, progress.newChild(30));
			replaceLiteralsWithConstant(expressionsToReplaceInSameContext, collector, progress.newChild(20));
			insertConstantDeclaration(collector, progress.newChild(10));
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}

	private void insertConstantDeclaration(ModificationCollector collector, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 10);
		try {
			MethodContext context = info.getMethodContext();
			IASTTranslationUnit ast = getAST(tu, progress.newChild(9));
			String constName = info.getName();
			if (context.getType() == MethodContext.ContextType.METHOD) {
				ICPPASTCompositeTypeSpecifier classDefinition = (ICPPASTCompositeTypeSpecifier) context.getMethodDeclaration().getParent();
				ClassMemberInserter.createChange(classDefinition, info.getVisibility(), createConstantDeclarationForClass(constName), true, collector);
			} else {
				IASTDeclaration nodes = createGlobalConstantDeclaration(constName, ast.getASTNodeFactory());
				ASTRewrite rewriter = collector.rewriterForTranslationUnit(ast);
				rewriter.insertBefore(ast, getFirstNode(ast), nodes, new TextEditGroup(Messages.ExtractConstantRefactoring_CreateConstant));
			}
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}

	private void replaceLiteralsWithConstant(Collection<IASTExpression> literals, ModificationCollector collector, IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, literals.size());
		String constName = info.getName();
		for (IASTExpression each : literals) {
			IASTTranslationUnit translationUnit = each.getTranslationUnit();
			ASTRewrite rewrite = collector.rewriterForTranslationUnit(translationUnit);
			INodeFactory nodeFactory = translationUnit.getASTNodeFactory();
			IASTIdExpression idExpression = nodeFactory.newIdExpression(nodeFactory.newName(constName.toCharArray()));
			rewrite.replace(each, idExpression, new TextEditGroup(Messages.ExtractConstantRefactoring_ReplaceLiteral));
			progress.worked(1);
		}
		if (monitor != null) {
			monitor.done();
		}
	}

	private Collection<IASTExpression> filterLiteralsInSameContext(Collection<IASTExpression> literalsToReplace, IProgressMonitor monitor) throws CoreException {
		try{
			SubMonitor progress = SubMonitor.convert(monitor);
			MethodContext context = info.getMethodContext();
			Collection<IASTExpression> locLiteralsToReplace = new ArrayList<IASTExpression>();
			if (context.getType() == MethodContext.ContextType.METHOD) {
				SubMonitor loopProgress = progress.setWorkRemaining(literalsToReplace.size());
				for (IASTExpression expression : literalsToReplace) {
					MethodContext exprContext = NodeHelper.findMethodContext(expression, refactoringContext, loopProgress.newChild(1));
					if (exprContext.getType() == MethodContext.ContextType.METHOD) {
						if (context.getMethodQName() != null) {
							if (MethodContext.isSameClass(exprContext.getMethodQName(), context.getMethodQName())) {
								locLiteralsToReplace.add(expression);
							}
						} else if (MethodContext.isSameClass(exprContext.getMethodDeclarationName(), context.getMethodDeclarationName())) {
							locLiteralsToReplace.add(expression);
						}
					}
				}
			} else {
				literalsToReplace.stream()
					.filter(expr -> expr.getTranslationUnit().getOriginatingTranslationUnit() != null)
					.collect(Collectors.toCollection(() -> locLiteralsToReplace));
			}
			return locLiteralsToReplace;
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}

	private Collection<IASTExpression> findExpressionsToExtract(IProgressMonitor monitor) throws OperationCanceledException, CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 10);
		final Collection<IASTExpression> result = new ArrayList<>();
		IASTTranslationUnit ast = getAST(tu, progress.newChild(8));
		ast.accept(new ASTVisitor() {
			{
				shouldVisitExpressions = true;
			}

			@Override
			public int visit(IASTExpression expression) {
				if (isSameExpressionTree(expression, target)) {
					if (!(expression.getNodeLocations().length == 1 &&
							expression.getNodeLocations()[0] instanceof IASTMacroExpansionLocation)) {
						result.add(expression);
					}
				}
				return super.visit(expression);
			}
		});
		progress.worked(3);
		return result;
	}

	private static boolean isSameExpressionTree(IASTExpression expression1, IASTExpression expression2) {
		if (expression1 instanceof IASTLiteralExpression && expression2 instanceof IASTLiteralExpression) {
			IASTLiteralExpression literalExpression1 = (IASTLiteralExpression) expression1;
			IASTLiteralExpression literalExpression2 = (IASTLiteralExpression) expression2;
			return literalExpression1.getKind() == literalExpression2.getKind() && String.valueOf(expression1).equals(String.valueOf(expression2));
		}
		if (expression1 instanceof IASTUnaryExpression && expression2 instanceof IASTUnaryExpression) {
			IASTUnaryExpression unaryExpression1 = (IASTUnaryExpression) expression1;
			IASTUnaryExpression unaryExpression2 = (IASTUnaryExpression) expression2;
			if (unaryExpression1.getOperator() == unaryExpression2.getOperator()) {
				return isSameExpressionTree(unaryExpression1.getOperand(), unaryExpression2.getOperand());
			}
		}
		if (expression1 instanceof IASTBinaryExpression && expression2 instanceof IASTBinaryExpression) {
			IASTBinaryExpression binaryExpression1 = (IASTBinaryExpression) expression1;
			IASTBinaryExpression binaryExpression2 = (IASTBinaryExpression) expression2;
			if (binaryExpression1.getOperator() == binaryExpression2.getOperator()) {
				return isSameExpressionTree(binaryExpression1.getOperand1(), binaryExpression2.getOperand1()) 
						&& isSameExpressionTree(binaryExpression1.getOperand2(), binaryExpression2.getOperand2());
			}
		}
		return false;
	}

	/**
	 * @return the first node in the translation unit or null
	 */
	private static IASTNode getFirstNode(IASTTranslationUnit ast) {
		IASTDeclaration[] declarations = ast.getDeclarations();
		return (declarations.length > 0) ? declarations[0] : null;
	}

	@Override
	protected RefactoringDescriptor getRefactoringDescriptor() {
		Map<String, String> arguments = getArgumentMap();
		RefactoringDescriptor desc = new ExtractConstantRefactoringDescriptor(project.getProject().getName(),
				"Extract Constant Refactoring", "Create constant for " + target.getRawSignature(), //$NON-NLS-1$ //$NON-NLS-2$
				arguments);
		return desc;
	}

	private Map<String, String> getArgumentMap() {
		Map<String, String> arguments = new HashMap<String, String>();
		arguments.put(CRefactoringDescriptor.FILE_NAME, tu.getLocationURI().toString());
		arguments.put(CRefactoringDescriptor.SELECTION, selectedRegion.getOffset() + "," + selectedRegion.getLength()); //$NON-NLS-1$
		arguments.put(ExtractConstantRefactoringDescriptor.NAME, info.getName());
		arguments.put(ExtractConstantRefactoringDescriptor.VISIBILITY, info.getVisibility().toString());
		return arguments;
	}

	private IASTSimpleDeclaration createConstantDeclaration(String newName) {
		ICPPNodeFactory factory = ASTNodeFactoryFactory.getDefaultCPPNodeFactory();
		DeclarationGenerator generator = DeclarationGenerator.create(factory);

		IType type = target.getExpressionType();

		IASTDeclSpecifier declSpec = generator.createDeclSpecFromType(type);
		declSpec.setConst(true);

		IASTDeclarator declarator = generator.createDeclaratorFromType(type, newName.toCharArray());

		IASTSimpleDeclaration simple = factory.newSimpleDeclaration(declSpec);

		IASTEqualsInitializer init = factory.newEqualsInitializer(target.copy());
		declarator.setInitializer(init);
		simple.addDeclarator(declarator);

		return simple;
	}

	private IASTDeclaration createGlobalConstantDeclaration(String newName, INodeFactory nodeFactory) {
		IASTSimpleDeclaration simple = createConstantDeclaration(newName);

		if (nodeFactory instanceof ICPPNodeFactory) {
			ICPPASTNamespaceDefinition namespace =
					((ICPPNodeFactory) nodeFactory).newNamespaceDefinition(nodeFactory.newName());
			namespace.addDeclaration(simple);
			return namespace;
		}

		simple.getDeclSpecifier().setStorageClass(IASTDeclSpecifier.sc_static);
		return simple;
	}

	private IASTDeclaration createConstantDeclarationForClass(String newName) {
		IASTSimpleDeclaration simple = createConstantDeclaration(newName);
		simple.getDeclSpecifier().setStorageClass(IASTDeclSpecifier.sc_static);
		return simple;
	}

	public ExtractConstantInfo getRefactoringInfo() {
		return info;
	}
}
