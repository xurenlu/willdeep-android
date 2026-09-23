package com.willdeep.android.ui

import com.willdeep.android.mobile.GatewaySession
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeWorkspaceGroupingTest {
    @Test
    fun groupsSessionsByWorkspaceAndPreservesGatewayOrder() {
        val sessions = listOf(
            session("a-4", "alpha"),
            session("b-2", "beta"),
            session("a-3", "alpha"),
            session("none", ""),
            session("a-2", "alpha"),
            session("b-1", "beta"),
            session("a-1", "alpha"),
        )

        val groups = groupSessionsByWorkspace(sessions, noWorkspaceLabel = "No workspace")

        assertEquals(listOf("alpha", "beta", NO_WORKSPACE_KEY), groups.map { it.key })
        assertEquals(listOf("a-4", "a-3", "a-2", "a-1"), groups[0].sessions.map { it.id })
        assertEquals(listOf("b-2", "b-1"), groups[1].sessions.map { it.id })
        assertEquals("No workspace", groups[2].label)
    }

    @Test
    fun collapsedWorkspaceShowsOnlyThreeNewestSessions() {
        val group = HomeWorkspaceSessionGroup(
            key = "alpha",
            label = "alpha",
            sessions = listOf("newest", "second", "third", "oldest").map { id ->
                session(id, "alpha")
            },
        )

        assertEquals(listOf("newest", "second", "third"), group.visibleSessions(expanded = false).map { it.id })
        assertEquals(
            listOf("newest", "second", "third", "oldest"),
            group.visibleSessions(expanded = true).map { it.id },
        )
    }

    @Test
    fun groupsDifferentWorkspaceFoldersUnderTheSameProject() {
        val sessions = listOf(
            session(
                id = "android",
                workspaceName = "willdeep-android",
                workspacePath = "/Users/rocky/Sites/willdeep-android",
                projectId = "project-1",
                projectName = "WillDeep",
            ),
            session(
                id = "mac",
                workspaceName = "Xedit",
                workspacePath = "/Users/rocky/Sites/Xedit",
                projectId = "project-1",
                projectName = "WillDeep",
            ),
        )

        val groups = groupSessionsByWorkspace(sessions, noWorkspaceLabel = "No workspace")

        assertEquals(1, groups.size)
        assertEquals("project:project-1", groups.single().key)
        assertEquals("WillDeep", groups.single().label)
        assertEquals(listOf("android", "mac"), groups.single().sessions.map { it.id })
    }

    private fun session(
        id: String,
        workspaceName: String,
        workspacePath: String = "",
        projectId: String? = null,
        projectName: String? = null,
    ): GatewaySession {
        return GatewaySession(
            id = id,
            title = id,
            workspaceName = workspaceName,
            messageCount = 1,
            isActive = false,
            isResponding = false,
            workspacePath = workspacePath,
            projectId = projectId,
            projectName = projectName,
        )
    }
}
