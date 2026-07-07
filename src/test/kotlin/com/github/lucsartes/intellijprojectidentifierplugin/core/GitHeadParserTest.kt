package com.github.lucsartes.intellijprojectidentifierplugin.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GitHeadParserTest {

    @Test
    fun branchFromHead_readsSymbolicRef() {
        assertEquals("main", GitHeadParser.branchFromHead("ref: refs/heads/main\n"))
    }

    @Test
    fun branchFromHead_keepsSlashesInBranchName() {
        assertEquals("feature/JIRA-123", GitHeadParser.branchFromHead("ref: refs/heads/feature/JIRA-123\n"))
    }

    @Test
    fun branchFromHead_returnsNullForDetachedHead() {
        assertNull(GitHeadParser.branchFromHead("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0\n"))
    }

    @Test
    fun branchFromHead_returnsNullForBlankOrGarbage() {
        assertNull(GitHeadParser.branchFromHead(""))
        assertNull(GitHeadParser.branchFromHead("not a head file"))
    }

    @Test
    fun gitDirFromDotGitFile_resolvesWorktreePointer() {
        assertEquals("../.git/worktrees/wt1", GitHeadParser.gitDirFromDotGitFile("gitdir: ../.git/worktrees/wt1\n"))
    }

    @Test
    fun gitDirFromDotGitFile_returnsNullWhenNotPointer() {
        assertNull(GitHeadParser.gitDirFromDotGitFile("ref: refs/heads/main"))
    }
}
