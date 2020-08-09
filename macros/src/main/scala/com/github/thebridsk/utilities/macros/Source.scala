package com.github.thebridsk.utilities.macros

object Source {
  import scala.language.experimental.macros

  /**
    * The name of the class where this is used
    */
  def className: String = macro Source.macroClassName

  /**
    * The name of the method where this is used
    */
  def methodName: String = macro Source.macroMethodName

  /**
    * The name of the source file where this is used.  example: Source.scala
    */
  def sourceFilename: String = macro Source.macroSourceFilename

  /**
    * The full name of the source file where this is used.
    * example: /source/utils/src/main/scala/utils/macros/Source.scala
    */
  def sourceFullFilename: String = macro Source.macroSourceFullFilename

  /**
    * The line number where this is used
    */
  def sourceLine: Int = macro Source.macroSourceLine
}

import scala.reflect.macros.blackbox.Context

/**
  * @author werewolf
  */
class Source(val c: Context) {

  /**
    * Get the Symbol that represents the method that is using the macro.
    * The compile is aborted if not found.
    * @return Symbol the MethodSymbol
    */
  def getMethodSymbol: Option[c.Symbol] = {
    import c.universe._
    def getMethodSymbolRecursively(sym: Symbol): Option[Symbol] = {
      if (sym == null || sym == NoSymbol || sym.owner == sym)
//        c.abort(c.enclosingPosition, "Macro must be used in a method. ")
        None
      else if (sym.isMethod)
        Some(sym)
      else
        getMethodSymbolRecursively(sym.owner)
    }
    getMethodSymbolRecursively(c.internal.enclosingOwner)
  }

  /**
    * Get a tree that represents the method, with signature.
    * @param methodSymbol must be a MethodSymbol
    * @return a Tree for the method name with signature
    */
  def getMethodName(maybeMethodSymbol: Option[c.Symbol]): c.Tree = {
    import c.universe._
    val line = macroSourceLine
    val file = macroSourceFilename
    maybeMethodSymbol match {
      case Some(methodSymbol) =>
        val methodName = methodSymbol.asMethod.name.toString
//        val params = methodSymbol.asMethod.paramLists.map( l => l.map(v => v.asTerm.name ).mkString("(", ",", ")")).mkString("", "", "")
        val m = methodName //+params //+":("+file+":"+line+")"
        // typeParams is always a nil list
        //    val typeParams = methodSymbol.asMethod.typeParams.mkString("(", ",", ")")
        //    val m = methodName+typeParams
        q"""($m+" ("+$file+":"+$line+")")"""
      case None => q""" "<unknown>:"+$line """
    }
  }

  /**
    * Get a tree that represents the method, with signature.
    * @param methodSymbol must be a MethodSymbol
    * @return a Tree for the method name with signature
    */
  def getMethodNameWithSignature(methodSymbol: c.Symbol): c.Tree = {
    import c.universe._
    val methodName = methodSymbol.asMethod.name.toString
    val typeParams = methodSymbol.asMethod.typeSignature
    val m = methodName + typeParams
    q"$m"
  }

  /**
    * Get the symbol for the class or module that is using the macro.
    * The compile is aborted if not found.
    * @return a ClassSymbol or ModuleSymbol
    */
  def getClassSymbol: c.Symbol = {
    import c.universe._
    def getClassSymbolRecursively(sym: Symbol): Symbol = {
      if (sym == null)
        c.abort(c.enclosingPosition, "Macro must be used in a class")
      else if (sym.isClass || sym.isModule)
        sym
      else
        getClassSymbolRecursively(sym.owner)
    }
    getClassSymbolRecursively(c.internal.enclosingOwner)
  }

  /**
    * Get a tree that represents the fully qualified class name.
    *
    * @param classSymbol should be either a ClassSymbol or a ModuleSymbol
    * @return a Tree for the classname
    */
  def getFullClassName(classSymbol: c.Symbol): c.Tree = {
    import c.universe._
    val className = classSymbol.fullName
    q"$className"
  }

  /**
    * The classname of where it is used
    * @return a Tree for the classname
    */
  def macroClassName: c.Tree = {
    getFullClassName(getClassSymbol)
  }

  /**
    * The method name of where it is used
    * @return a Tree for the method name
    */
  def macroMethodName: c.Tree = {
    getMethodName(getMethodSymbol)
  }

  /**
    * The source filename of where it is used
    * @return a Tree for the source filename
    */
  def macroSourceFilename: c.Tree = {
    import c._, universe._;
    Literal(Constant(c.enclosingPosition.source.file.name))
  }

  /**
    * The source full filename of where it is used
    * @return a Tree for the the source full filename
    */
  def macroSourceFullFilename: c.Tree = {
    import c._, universe._;
    Literal(Constant(c.enclosingPosition.source.file.path))
  }

  /**
    * The source line number of where it is used
    * @return a Tree for the source line number
    */
  def macroSourceLine: c.Tree = {
    import c._, universe._;
    Literal(Constant(c.enclosingPosition.line))
  }

}
