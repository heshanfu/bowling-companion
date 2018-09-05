package ca.josephroque.bowlingcompanion.teams.teammember

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ca.josephroque.bowlingcompanion.common.fragments.ListFragment
import ca.josephroque.bowlingcompanion.teams.Team
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import java.util.Collections

/**
 * Copyright (C) 2018 Joseph Roque
 *
 * A fragment to display a list of team members.
 */
class TeamMembersListFragment :
    ListFragment<TeamMember, TeamMembersRecyclerViewAdapter>(),
    TeamMembersRecyclerViewAdapter.TeamMemberMoveDelegate {

    companion object {
        /** Logging identifier. */
        @Suppress("unused")
        private const val TAG = "TeamMembersListFragment"

        /** Identifier for the argument that represents the [Team] whose details are displayed. */
        private const val ARG_TEAM = "${TAG}_team"

        /**
         * Creates a new instance.
         *
         * @param team team to load details of
         * @return the new instance
         */
        fun newInstance(team: Team): TeamMembersListFragment {
            val fragment = TeamMembersListFragment()
            fragment.arguments = Bundle().apply { putParcelable(ARG_TEAM, team) }
            return fragment
        }
    }

    /** The team whose details are to be displayed. */
    private var team: Team? = null

    /** Interaction delegate. */
    private var teamMemberDelegate: TeamMemberListFragmentDelegate? = null

    /** Indicates if all team members are ready to begin a game. */
    private val allTeamMembersReady: Boolean
        get() = (adapter?.items?.filter { it.league != null }?.size ?: -1) == (adapter?.items?.size ?: -2)

    /** @Override */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        team = arguments?.getParcelable(ARG_TEAM)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    /** @Override */
    override fun updateToolbarTitle() {
        // Intentionally left blank
    }

    /** @Override */
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        val parent = parentFragment as? TeamMemberListFragmentDelegate ?: throw RuntimeException("${parentFragment!!} must implement TeamMemberListFragmentDelegate")
        teamMemberDelegate = parent
    }

    /** @Override */
    override fun onDetach() {
        super.onDetach()
        teamMemberDelegate = null
    }

    /** @Override */
    override fun buildAdapter(): TeamMembersRecyclerViewAdapter {
        val teamMembers: List<TeamMember> = team?.members ?: emptyList()
        val teamMembersOrder: List<Long> = team?.order ?: emptyList()
        return TeamMembersRecyclerViewAdapter(teamMembers, teamMembersOrder, this, this)
    }

    /** @Override */
    override fun fetchItems(): Deferred<MutableList<TeamMember>> {
        return async(CommonPool) {
            team?.let {
                return@async it.membersInOrder.toMutableList()
            }
            mutableListOf<TeamMember>()
        }
    }

    /** @Override */
    override fun listWasRefreshed() {
        teamMemberDelegate?.onTeamMembersReadyChanged(allTeamMembersReady)
    }

    /** @Override */
    override fun onTeamMemberMoved(from: Int, to: Int) {
        val team = team ?: return
        val teamMemberOrder = team.order.toMutableList()
        Collections.swap(teamMemberOrder, from, to)

        // Update the team with the new order
        this@TeamMembersListFragment.team = Team(
                id = team.id,
                name = team.name,
                members = team.members,
                initialOrder = teamMemberOrder)
        arguments?.putParcelable(ARG_TEAM, this@TeamMembersListFragment.team)

        // Update adapter and delegate
        adapter?.itemsOrder = teamMemberOrder
        adapter?.notifyItemMoved(from, to)
        teamMemberDelegate?.onTeamMembersReordered(teamMemberOrder)
    }

    /**
     * Handle interactions with the list.
     */
    interface TeamMemberListFragmentDelegate {

        /**
         * Called when the team members that are ready change.
         *
         * @param ready true when all team members are ready to bowl, false otherwise.
         */
        fun onTeamMembersReadyChanged(ready: Boolean)

        /**
         * Called when the user re-orders the team members.
         *
         * @param order the new order of team members
         */
        fun onTeamMembersReordered(order: List<Long>)
    }
}
