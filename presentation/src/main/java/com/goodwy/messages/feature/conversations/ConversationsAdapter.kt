package com.goodwy.messages.feature.conversations

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.goodwy.messages.R
import com.goodwy.messages.common.Navigator
import com.goodwy.messages.common.base.QkRealmAdapter
import com.goodwy.messages.common.base.QkViewHolder
import com.goodwy.messages.common.util.Colors
import com.goodwy.messages.common.util.DateFormatter
import com.goodwy.messages.common.util.extensions.dpToPx
import com.goodwy.messages.common.util.extensions.getColorCompat
import com.goodwy.messages.common.util.extensions.resolveThemeColor
import com.goodwy.messages.common.util.extensions.setTint
import com.goodwy.messages.common.util.extensions.setVisible
import com.goodwy.messages.compat.SubscriptionManagerCompat
import com.goodwy.messages.model.Conversation
import com.goodwy.messages.util.PhoneNumberUtils
import com.goodwy.messages.util.Preferences
import kotlinx.android.synthetic.main.conversation_list_item.*
import kotlinx.android.synthetic.main.conversation_list_item.simIndex
import kotlinx.android.synthetic.main.conversation_list_item.sim
import kotlinx.android.synthetic.main.conversation_list_item.view.*
import kotlinx.android.synthetic.main.message_list_item_in.*
import javax.inject.Inject

class ConversationsAdapter @Inject constructor(
    subscriptionManager: SubscriptionManagerCompat,
    private val colors: Colors,
    private val context: Context,
    private val dateFormatter: DateFormatter,
    private val navigator: Navigator,
    private val phoneNumberUtils: PhoneNumberUtils,
    private val prefs: Preferences
) : QkRealmAdapter<Conversation>() {

    init {
        // This is how we access the threadId for the swipe actions
        setHasStableIds(true)
    }

    private val subs = subscriptionManager.activeSubscriptionInfoList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.conversation_list_item, parent, false)

        if (viewType == 1) {
            val textColorPrimary = parent.context.resolveThemeColor(android.R.attr.textColorPrimary)

            view.title.setTypeface(view.title.typeface, Typeface.BOLD)

            view.snippet.setTypeface(view.snippet.typeface, Typeface.BOLD)
            view.snippet.setTextColor(textColorPrimary)
            view.snippet.maxLines = 5

            view.unread.isVisible = true

            view.date.setTypeface(view.date.typeface, Typeface.BOLD)
            view.date.setTextColor(textColorPrimary)
        }

        return QkViewHolder(view).apply {
            view.setOnClickListener {
                val conversation = getItem(adapterPosition) ?: return@setOnClickListener
                when (toggleSelection(conversation.id, false)) {
                    true -> view.isActivated = isSelected(conversation.id)
                    false -> navigator.showConversation(conversation.id)
                }
            }

            view.setOnLongClickListener {
                val conversation = getItem(adapterPosition) ?: return@setOnLongClickListener true
                if (hasSelection()) {
                    toggleSelection(conversation.id)
                    view.isActivated = isSelected(conversation.id)
                } else {
                    val act = (context as? Activity) ?: (view.context as? Activity)
                    if (act != null) {
                        ConversationAppearanceDialog.show(
                            activity = act,
                            conversation = conversation,
                            prefs = prefs,
                            onAppearanceChanged = { notifyItemChanged(adapterPosition) },
                            onSelectClicked = {
                                toggleSelection(conversation.id)
                                view.isActivated = isSelected(conversation.id)
                            }
                        )
                    } else {
                        toggleSelection(conversation.id)
                        view.isActivated = isSelected(conversation.id)
                    }
                }
                true
            }
        }
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val conversation = getItem(position) ?: return

        // If the last message wasn't incoming, then the colour doesn't really matter anyway
        val lastMessage = conversation.lastMessage
        val recipient = when {
            conversation.recipients.size == 1 || lastMessage == null -> conversation.recipients.firstOrNull()
            else -> conversation.recipients.find { recipient ->
                phoneNumberUtils.compare(recipient.address, lastMessage.address)
            }
        }
        val theme = colors.theme(recipient).theme

        holder.containerView.isActivated = isSelected(conversation.id)

        // Custom Per-Contact Glass Color & Height Customization
        val colorKey = "custom_cell_color_${conversation.id}"
        val heightKey = "custom_cell_height_${conversation.id}"
        val customTint = prefs.rxPrefs.getInteger(colorKey, 0).get()
        val customHeight = prefs.rxPrefs.getInteger(heightKey, 0).get()

        when (customHeight) {
            1 -> { // Compact
                holder.containerView.setPadding(12.dpToPx(context), 6.dpToPx(context), 14.dpToPx(context), 6.dpToPx(context))
                holder.containerView.minimumHeight = 56.dpToPx(context)
            }
            2 -> { // Comfortable
                holder.containerView.setPadding(12.dpToPx(context), 16.dpToPx(context), 14.dpToPx(context), 16.dpToPx(context))
                holder.containerView.minimumHeight = 88.dpToPx(context)
            }
            3 -> { // Spacious
                holder.containerView.setPadding(12.dpToPx(context), 22.dpToPx(context), 14.dpToPx(context), 22.dpToPx(context))
                holder.containerView.minimumHeight = 104.dpToPx(context)
            }
            else -> { // Standard (Default)
                holder.containerView.setPadding(12.dpToPx(context), 12.dpToPx(context), 14.dpToPx(context), 12.dpToPx(context))
                holder.containerView.minimumHeight = 72.dpToPx(context)
            }
        }

        if (customTint != 0) {
            holder.containerView.background = createCustomGlassDrawable(context, customTint, isSelected(conversation.id))
        } else {
            holder.containerView.setBackgroundResource(R.drawable.glass_card_conversation)
        }

        holder.avatars.recipients = conversation.recipients
        holder.title.collapseEnabled = conversation.recipients.size > 1
        holder.title.text = buildSpannedString {
            append(conversation.getTitle())
            if (conversation.draft.isNotEmpty()) {
                color(theme) { append(" " + context.getString(R.string.main_draft)) }
            }
        }

        val subscription = subs.find { sub -> sub.subscriptionId == lastMessage?.subId }
        holder.simIndex.text = subscription?.simSlotIndex?.plus(1)?.toString()
        holder.sim.setVisible(subscription != null && subs.size > 1)
        holder.simIndex.setVisible(subscription != null && subs.size > 1)

        val simColor = when (subscription?.simSlotIndex?.plus(1)?.toString()) {
            "1" -> colors.colorForSim(context, 1)
            "2" -> colors.colorForSim(context, 2)
            "3" -> colors.colorForSim(context, 3)
            else -> colors.colorForSim(context, 1)
        }
        if (prefs.simColor.get()) {
            holder.sim.setTint(simColor)
        }

        holder.date.text = conversation.date.takeIf { it > 0 }?.let(dateFormatter::getConversationTimestamp)
        holder.snippet.text = when {
            conversation.draft.isNotEmpty() -> conversation.draft
            conversation.me -> context.getString(R.string.main_sender_you, conversation.snippet)
            else -> conversation.snippet
        }
        holder.pinned.isVisible = conversation.pinned
        holder.unread.setTint(theme)
        if (prefs.separator.get()) {
            holder.separator.isVisible = true
        } else {
            holder.separator.isVisible = false
        }
    }

    private fun createCustomGlassDrawable(context: Context, tintColor: Int, isActivated: Boolean): Drawable {
        val radius = 20.dpToPx(context).toFloat()

        val shadow = GradientDrawable().apply {
            cornerRadius = radius
            setColor(0x2A050B14.toInt())
        }

        val glassBody = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                tintColor,
                tintColor and 0x00FFFFFF or 0x14000000,
                tintColor and 0x00FFFFFF or 0x0A000000
            )
        ).apply {
            cornerRadius = radius
            if (isActivated) {
                setStroke(2.dpToPx(context), 0xFF007AFF.toInt())
            } else {
                setStroke(1.5.dpToPx(context).toInt(), 0x66FFFFFF.toInt())
            }
        }

        val glare = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0x66FFFFFF.toInt(), 0x1AFFFFFF.toInt(), 0x00FFFFFF.toInt())
        ).apply {
            cornerRadii = floatArrayOf(radius, radius, radius, radius, 8f, 8f, 8f, 8f)
        }

        return LayerDrawable(arrayOf(shadow, glassBody, glare)).apply {
            setLayerInset(0, 1.dpToPx(context), 2.dpToPx(context), 1.dpToPx(context), 0)
            setLayerInset(1, 0, 0, 0, 2.dpToPx(context))
            setLayerInset(2, 4.dpToPx(context), 1.dpToPx(context), 4.dpToPx(context), 20.dpToPx(context))
        }
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)?.id ?: -1
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position)?.unread == false) 0 else 1
    }
}
