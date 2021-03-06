package ca.josephroque.bowlingcompanion.games.overview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import ca.josephroque.bowlingcompanion.R
import ca.josephroque.bowlingcompanion.common.fragments.ListFragment
import ca.josephroque.bowlingcompanion.common.interfaces.IFloatingActionButtonHandler
import ca.josephroque.bowlingcompanion.common.interfaces.IIdentifiable
import ca.josephroque.bowlingcompanion.games.Game
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import android.support.v7.app.AlertDialog
import ca.josephroque.bowlingcompanion.statistics.interfaces.IStatisticsContext
import ca.josephroque.bowlingcompanion.statistics.provider.StatisticsProvider
import ca.josephroque.bowlingcompanion.utils.Permission
import ca.josephroque.bowlingcompanion.utils.ShareUtils

/**
 * Copyright (C) 2018 Joseph Roque
 *
 * A fragment to display an overview of a list of games.
 */
class GameOverviewFragment : ListFragment<Game, GameOverviewRecyclerViewAdapter>(),
        ListFragment.ListFragmentDelegate,
        IFloatingActionButtonHandler,
        IStatisticsContext {

    companion object {
        @Suppress("unused")
        private const val TAG = "GameOverviewFragment"

        private const val ARG_GAMES = "${TAG}_games"

        fun newInstance(games: List<Game>): GameOverviewFragment {
            val fragment = GameOverviewFragment()
            fragment.arguments = Bundle().apply {
                putParcelableArrayList(ARG_GAMES, ArrayList(games))
            }
            return fragment
        }

        enum class ShareOption {
            Share, Save;

            companion object {
                private val map = ShareOption.values().associateBy(ShareOption::ordinal)
                fun fromInt(type: Int) = map[type]
            }

            val title: Int
                get() {
                    return when (this) {
                        Share -> R.string.share
                        Save -> R.string.save_to_device
                    }
                }
        }
    }

    private lateinit var games: List<Game>

    override val emptyViewImage = R.drawable.empty_view_leagues
    override val emptyViewText = R.string.empty_view_game_overview

    private var externalPermissionsGrantedCallback: (() -> Unit)? = null

    private var isSharing: Boolean = false
        set(value) {
            field = value
            adapter?.multiSelect = isSharing

            if (isSharing) {
                adapter?.setSelectedElementsWithIds(HashSet(games.map { it.id }))
                headerTitle = R.string.sharing_instructions_title
                headerSubtitle = R.string.sharing_instructions_body
            } else {
                headerTitle = null
                headerSubtitle = null
            }
            fabProvider?.invalidateFab()
        }

    override val statisticsProviders: List<StatisticsProvider> by lazy {
        val providers: MutableList<StatisticsProvider> = arrayListOf(
                StatisticsProvider.BowlerStatistics(games[0].series.league.bowler),
                StatisticsProvider.LeagueStatistics(games[0].series.league),
                StatisticsProvider.SeriesStatistics(games[0].series)
        )

        providers.addAll(games.map { StatisticsProvider.GameStatistics(it) })
        return@lazy providers
    }

    // MARK: Lifecycle functions

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        arguments?.let { games = it.getParcelableArrayList(ARG_GAMES)!! }
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onAttach(context: Context?) {
        canIgnoreDelegate = true
        delegate = this
        super.onAttach(context)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_game_overview, menu)

        menu.findItem(R.id.action_share).isVisible = !isSharing
        menu.findItem(R.id.action_stop_sharing).isVisible = isSharing
    }

    override fun onDestroy() {
        super.onDestroy()
        externalPermissionsGrantedCallback = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                isSharing = true
                activity?.invalidateOptionsMenu()
                true
            }
            R.id.action_stop_sharing -> {
                isSharing = false
                activity?.invalidateOptionsMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // MARK: BaseFragment

    override fun updateToolbarTitle() {
        context?.let { navigationActivity?.setToolbarTitle(it.resources.getString(R.string.overview)) }
    }

    // MARK: ListFragment

    override fun buildAdapter(): GameOverviewRecyclerViewAdapter {
        val adapter = GameOverviewRecyclerViewAdapter(emptyList(), this)
        adapter.swipeable = false
        adapter.longPressable = true
        return adapter
    }

    override fun fetchItems(): Deferred<MutableList<Game>> {
        return async(CommonPool) {
            return@async this@GameOverviewFragment.games.toMutableList()
        }
    }

    // MARK: AdapterDelegate

    override fun onItemSelected(item: IIdentifiable, longPress: Boolean) {
        if (item is Game) {
            if (longPress && adapter?.multiSelect == true) {
                adapter?.setSelectedElementsWithIds(setOf(item.id))
                promptShareGames()
            }
        }
    }

    override fun onItemDeleted(item: IIdentifiable) {
        // Intentionally left blank
    }

    // MARK: IFloatingActionButtonHandler

    override fun getFabImage(): Int? {
        return if (isSharing) R.drawable.ic_share else null
    }

    override fun onFabClick() {
        if (isSharing) {
            promptShareGames()
        }
    }

    override fun permissionGranted(permission: Permission) {
        when (permission) {
            Permission.WriteExternalStorage -> externalPermissionsGrantedCallback?.invoke()
        }
    }

    // MARK: Private functions

    private fun promptShareGames() {
        val activity = activity ?: return
        val sortedGames = adapter?.selectedItemsInOrder ?: return
        val options = ShareOption.values().map { activity.resources.getString(it.title) }

        val shareBuilder = AlertDialog.Builder(activity)
        shareBuilder.setTitle(R.string.share_or_save)
                .setSingleChoiceItems(options.toTypedArray(), ShareOption.Share.ordinal, null)
                .setPositiveButton(R.string.okay) { dialog, _ ->
                    if (dialog is AlertDialog) {
                        val selectedItem = ShareOption.fromInt(dialog.listView.checkedItemPosition)!!
                        when (selectedItem) {
                            ShareOption.Share -> ShareUtils.shareGames(activity, sortedGames)
                            ShareOption.Save -> {
                                externalPermissionsGrantedCallback = {
                                    ShareUtils.saveGames(activity, sortedGames)
                                    externalPermissionsGrantedCallback = null
                                }
                                ShareUtils.saveGames(activity, sortedGames)
                            }
                        }
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
    }
}
