package com.example.instainsights

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.instainsights.databinding.ActivityPostInsightsBinding
import com.example.instainsights.models.DataXX
import com.example.instainsights.models.postModels.ContentStats
import com.example.instainsights.models.postModels.PostPerformance
import com.example.instainsights.models.postModels.PostSettings
import com.example.instainsights.models.postModels.ReelStats
import com.example.instainsights.models.postModels.SentimentAnalysis
import com.example.instainsights.viewmodel.PostInsightsData
import com.example.instainsights.viewmodel.PostInsightsViewModel
import com.example.instainsights.viewmodel.PostInsightsViewModelFactory
import com.example.instainsights.viewmodel.UiState
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class PostInsightsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_POST = "extra_post"
    }

    private lateinit var binding: ActivityPostInsightsBinding
    private lateinit var post: DataXX

    private val viewModel: PostInsightsViewModel by viewModels {
        PostInsightsViewModelFactory(this, post.id, post.media_type)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPostInsightsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        post = intent.getSerializableExtra(EXTRA_POST) as DataXX
            ?: run { finish(); return }

        setupToolbar()
        bindPostHeader()
        setupRetry()
        setupPostSettingsCard()
        observeUiState()
        observeSaveState()
    }

    // ── Toolbar ──────────────────────────────────────────────────────────────

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Post insights"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // ── Post header (filled immediately from Intent — no network wait) ────────

    private fun bindPostHeader() {
        // These IDs live directly in activity_post_insights.xml (card_post_header
        // is merged inline, NOT via <include>) so binding.xyz works directly.
        Glide.with(this)
            .load(post.media_url)
            .centerCrop()
            .placeholder(R.drawable.ic_auto_reply)
            .into(binding.ivPostThumbnail)

        binding.tvCaption.text = post.caption ?: "No caption"
        binding.tvTimestamp.text = formatDate(post.timestamp)
        binding.tvMediaType.text = when (post.media_type) {
            "VIDEO"          -> "Reel"
            "CAROUSEL_ALBUM" -> "Carousel"
            else             -> "Photo"
        }
        binding.tvLikeCount.text    = formatCount(post.like_count)
        binding.tvCommentCount.text = formatCount(post.comments_count)
    }

    private fun setupPostSettingsCard() {
        val card = binding.cardPostSettings

        // Show/hide input fields when radio selection changes
        card.rgReplyMode.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbReplyNo -> {
                    card.tilStaticMessage.visibility = View.GONE
                    card.tilAiPrompt.visibility      = View.GONE
                }
                R.id.rbReplyStatic -> {
                    card.tilStaticMessage.visibility = View.VISIBLE
                    card.tilAiPrompt.visibility      = View.GONE
                    card.etStaticMessage.requestFocus()
                }
                R.id.rbReplyAi -> {
                    card.tilStaticMessage.visibility = View.GONE
                    card.tilAiPrompt.visibility      = View.VISIBLE
                    card.etAiPrompt.requestFocus()
                }
            }
        }

        // Save button
        card.btnSaveSettings.setOnClickListener {
            val mode = when (card.rgReplyMode.checkedRadioButtonId) {
                R.id.rbReplyStatic -> "static"
                R.id.rbReplyAi     -> "ai"
                else               -> "no"
            }

            val message = when (mode) {
                "static" -> card.etStaticMessage.text?.toString()?.trim()
                "ai"     -> card.etAiPrompt.text?.toString()?.trim()
                else     -> null
            }

            // Validate before sending
            if (mode == "static" && message.isNullOrEmpty()) {
                card.tilStaticMessage.error = "Please enter a reply message"
                return@setOnClickListener
            }
            if (mode == "ai" && message.isNullOrEmpty()) {
                card.tilAiPrompt.error = "Please enter an AI prompt"
                return@setOnClickListener
            }

            card.tilStaticMessage.error = null
            card.tilAiPrompt.error      = null

            viewModel.savePostSettings(
                postId          = post.id,
                enableAutoHide  = card.switchAutoHide.isChecked,
                enableAutoreply = mode,
                message         = message
            )
        }
    }

    // ── Retry button ──────────────────────────────────────────────────────────

    private fun setupRetry() {
        binding.btnRetry.setOnClickListener { viewModel.load() }
    }

    private fun bindPostSettings(settings: PostSettings) {
        val card = binding.cardPostSettings

        // Set toggle state
        card.switchAutoHide.isChecked = settings.enableAutoHide

        // Set radio button and pre-fill text
        when (settings.enableAutoreply) {
            "static" -> {
                card.rgReplyMode.check(R.id.rbReplyStatic)
                card.tilStaticMessage.visibility = View.VISIBLE
                card.tilAiPrompt.visibility      = View.GONE
                card.etStaticMessage.setText(settings.message ?: "")
            }
            "ai" -> {
                card.rgReplyMode.check(R.id.rbReplyAi)
                card.tilStaticMessage.visibility = View.GONE
                card.tilAiPrompt.visibility      = View.VISIBLE
                card.etAiPrompt.setText(settings.message ?: "")
            }
            else -> {
                card.rgReplyMode.check(R.id.rbReplyNo)
                card.tilStaticMessage.visibility = View.GONE
                card.tilAiPrompt.visibility      = View.GONE
            }
        }
    }


    // ── State observer ────────────────────────────────────────────────────────
    private fun observeSaveState() {
        lifecycleScope.launch {
            viewModel.saveState.collect { state ->
                val card = binding.cardPostSettings
                when (state) {
                    is PostInsightsViewModel.SaveState.Saving -> {
                        card.btnSaveSettings.isEnabled = false
                        card.btnSaveSettings.text      = "Saving…"
                    }
                    is PostInsightsViewModel.SaveState.Saved -> {
                        card.btnSaveSettings.isEnabled = true
                        card.btnSaveSettings.text      = "Save settings"
                        card.tvSavedBadge.visibility   = View.VISIBLE
                    }
                    is PostInsightsViewModel.SaveState.Error -> {
                        card.btnSaveSettings.isEnabled = true
                        card.btnSaveSettings.text      = "Save settings"
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                    }
                    is PostInsightsViewModel.SaveState.Idle -> {
                        card.tvSavedBadge.visibility = View.GONE
                    }
                }
            }
        }
    }
    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UiState.Loading -> showLayout(loading = true)
                    is UiState.Error   -> {
                        showLayout(error = true)
                        binding.tvErrorMessage.text = state.message
                    }
                    is UiState.Success -> {
                        showLayout(content = true)
                        bindInsights(state.data)
                    }
                }
            }
        }
    }

    private fun showLayout(
        loading: Boolean = false,
        error  : Boolean = false,
        content: Boolean = false
    ) {
        binding.layoutLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.layoutError.visibility   = if (error)   View.VISIBLE else View.GONE
        binding.layoutContent.visibility = if (content) View.VISIBLE else View.GONE
    }

    // ── Top-level binder ─────────────────────────────────────────────────────

    private fun bindInsights(data: PostInsightsData) {
        bindContentStats(data.contentStats)
        bindPerformance(data.performance)
        bindSentiment(data.sentiment)

        if (data.reelStats != null) {
            binding.sectionReel.visibility = View.VISIBLE
            bindReelStats(data.reelStats)
        } else {
            binding.sectionReel.visibility = View.GONE
        }
    }

    // ── Content stats ─────────────────────────────────────────────────────────
    // Views live inside card_content_stats.xml which is pulled in via <include
    // android:id="@+id/cardContentStats"> so we access them through that sub-binding.

    private fun bindContentStats(stats: ContentStats) {
        val map = stats.data.associate { it.name to (it.values.firstOrNull()?.value ?: 0) }

        // binding.cardContentStats is the binding object for the included layout
        val s = binding.cardContentStats
        s.tvReach.text            = formatCount(map["reach"]              ?: 0)
        s.tvViews.text            = formatCount(map["views"]              ?: 0)
        s.tvInteractions.text     = formatCount(map["total_interactions"] ?: 0)
        s.tvShares.text           = formatCount(map["shares"]             ?: 0)
        s.tvFollowsFromPost.text  = formatCount(map["follows"]            ?: 0)
    }

    // ── Performance metrics ───────────────────────────────────────────────────

    private fun bindPerformance(perf: PostPerformance) {
        val p = binding.cardPerformance
        p.tvEngagementRate.text = perf.insights.engagementRate
        p.tvVelocity.text       = perf.insights.performanceVelocity
        p.tvViralityScore.text  = perf.insights.viralityScore
    }

    // ── Reel stats ────────────────────────────────────────────────────────────

    private fun bindReelStats(reelStats: ReelStats) {
        val r   = binding.cardReelStats
        val map = reelStats.data.associate { it.name to (it.values.firstOrNull()?.value ?: 0) }

        r.tvSkipRate.text = "${map["reels_skip_rate"] ?: 0}%"

        // Retention fields are left as "—" here because ReelRetention is not
        // fetched in your current ViewModel. Wire them up once you add that call.
    }

    // ── Comment sentiment ─────────────────────────────────────────────────────

    private fun bindSentiment(sentiment: SentimentAnalysis) {
        val cs = binding.cardSentiment          // sub-binding for card_sentiment.xml
        cs.tvSentimentSummary.text = sentiment.overall.summary
        cs.tvSentimentScore.text   = "%.2f".format(sentiment.overall.score)
        cs.tvSentimentLabel.text   = sentiment.overall.sentiment
            .replaceFirstChar { it.uppercase() }

        // Colour the circular score chip based on overall mood
        val chipColor = when (sentiment.overall.sentiment) {
            "positive" -> R.color.accent_follows
            "negative" -> R.color.error
            else       -> R.color.warning
        }
        cs.cardSentimentScore.setCardBackgroundColor(
            ContextCompat.getColor(this, chipColor)
        )

        // Breakdown progress bars
        val total = sentiment.totalComments.coerceAtLeast(1)
        cs.progressPositive.progress = sentiment.overall.breakdown.positive * 100 / total
        cs.progressNegative.progress = sentiment.overall.breakdown.negative * 100 / total
        cs.progressNeutral.progress  = sentiment.overall.breakdown.neutral  * 100 / total

        cs.tvPositiveCount.text = sentiment.overall.breakdown.positive.toString()
        cs.tvNegativeCount.text = sentiment.overall.breakdown.negative.toString()
        cs.tvNeutralCount.text  = sentiment.overall.breakdown.neutral.toString()



    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun formatCount(count: Int): String = when {
        count >= 1_000_000 -> "%.1fM".format(count / 1_000_000f)
        count >= 1_000     -> "%.1fK".format(count / 1_000f)
        else               -> count.toString()
    }

    private fun formatDate(iso: String): String = try {
        val parser    = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
        val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        formatter.format(parser.parse(iso)!!)
    } catch (e: Exception) { iso }
}