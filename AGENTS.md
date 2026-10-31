# Agent instructions

## Changing the lexer or parser

When `src/main/java/org/asciidoc/intellij/lexer/asciidoc.flex` or the parser changes, bump the following version numbers to force the IDE to rebuild its indexes.
Bump them only once per release: first compare them with the last release tag (for example `git show <last-tag>:<file>`), and skip the bump if they already differ, as the unreleased version has never been installed with the older numbers.

* `AsciiDocFileElementType.getStubVersion()` in `src/main/java/org/asciidoc/intellij/parser/AsciiDocFileElementType.java` (the string stub index version is derived from it)
* `AsciiDocIdIndexer.getVersion()` in `src/main/java/org/asciidoc/intellij/indexer/AsciiDocIdIndexer.java`
* `AsciiDocTodoIndexer.getVersion()` in `src/main/java/org/asciidoc/intellij/indexer/AsciiDocTodoIndexer.java`
* `AsciiDocWordsScanner.getVersion()` in `src/main/java/org/asciidoc/intellij/parser/AsciiDocWordsScanner.java`

Add a lexer test in `AsciiDocLexerTest` for the change, including negative cases.
