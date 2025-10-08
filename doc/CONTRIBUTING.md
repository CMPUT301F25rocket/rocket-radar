# Issue labels
- *Subject* (S): What are we talking about?
- *Priority* (P): How important?
- *Difficulty* (D): How hard?
- *Flags* (F): Additional information. Is the issue a question? Is it due by
  checkpoint? etc.

# Commit format
We are going to try to follow <https://www.conventionalcommits.org/en/v1.0.0/>
ideally. That means our messages are structured `<type>[(scope)]:
<description>` where `<description>` is written in active present tense (e.g.
"fixed the computing of special coefficient" becomes "fix special coefficient
computation") since it tends to require fewer characters to write and nicer to
interpret since the commit history can be read as a sequence of actions on the
codebase.

Here are some type definitions:

- `feat` -- Making new functionality.
- `fix` -- Correcting broken functionality.
- `refactor` -- Changing code structure and organization but not functionality.
- `perf` -- Make things go faster but still work the same.
- `docs` -- Code comments, UML, etc.
- `test` -- Writing and updating tests.
- `ci` -- Usually just for changing something in `.github/workflows`.
- `chore` -- Misc stuff

Type definitions exist to encourage atomic commits. Making changes to a
specific set of files at a time and then commiting. If people commit often
(**and push those commits**) we can reduce the likelyhood of merge conflicts.
And the ones we get should be relatively easy to resolve since the majority of
commits won't be in conflict.

One modification that can be handy for Github is a modification to the scope to
associate commits to specific issues.

If we have a docs issue #15 about a missing class from the UML we and have a
commit which addresses it a message could be written something like: `docs(uml;
close #15): add missing class`. And if this is commited to the default branch 
Github will then automatically close issue #15. We can also just link regularly
with `docs(uml; #15): ...` and then the commit will be linked to the issue but
won't close it.

Any of the following words followed by an issue number will have the same effect:
- `close`
- `closes`
- `closed`
- `fix`
- `fixes`
- `fixed`
- `resolve`
- `resolves`
- `resolved`


