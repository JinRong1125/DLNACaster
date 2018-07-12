package tti.com.ttidlnacaster.dlna.action

import android.content.Intent
import java.io.Serializable

/**
 * Created by dylan_liang on 2018/2/9.
 */
object Intents {
    /**
     * Prefix for all intents created
     */
    val INTENT_PREFIX = "com.kevinshen.beyondupup."

    /**
     * Prefix for all extra data added to intents
     */
    val INTENT_EXTRA_PREFIX = INTENT_PREFIX + "extra."

    /**
     * Prefix for all action in intents
     */
    val INTENT_ACTION_PREFIX = INTENT_PREFIX + "action."

    /**
     * Playing action for MediaPlayer
     */
    val ACTION_PLAYING = INTENT_ACTION_PREFIX + "playing"

    /**
     * Paused playback action for MediaPlayer
     */
    val ACTION_PAUSED_PLAYBACK = INTENT_ACTION_PREFIX + "paused_playback"

    /**
     * Stopped action for MediaPlayer
     */
    val ACTION_STOPPED = INTENT_ACTION_PREFIX + "stopped"

    /**
     * Change device action for MediaPlayer
     */
    val ACTION_CHANGE_DEVICE = INTENT_ACTION_PREFIX + "change_device"

    /**
     * Set volume action for MediaPlayer
     */
    val ACTION_SET_VOLUME = INTENT_ACTION_PREFIX + "set_volume"

    /**
     * Update the lastChange value action for MediaPlayer
     */
    val ACTION_UPDATE_LAST_CHANGE = INTENT_ACTION_PREFIX + "update_last_change"

    /**
     * Builder for generating an intent configured with extra data.
     */
    class Builder
    /**
     * Create builder with suffix
     *
     * @param actionSuffix
     */
    (actionSuffix: String) {

        private val intent: Intent

        init {
            intent = Intent(INTENT_PREFIX + actionSuffix)
        }

        /**
         * Add extra field data value to intent being built up
         *
         * @param fieldName
         * @param value
         * @return this builder
         */
        fun add(fieldName: String, value: String): Builder {
            intent.putExtra(fieldName, value)
            return this
        }

        /**
         * Add extra field data values to intent being built up
         *
         * @param fieldName
         * @param values
         * @return this builder
         */
        fun add(fieldName: String, values: Array<CharSequence>): Builder {
            intent.putExtra(fieldName, values)
            return this
        }

        /**
         * Add extra field data value to intent being built up
         *
         * @param fieldName
         * @param value
         * @return this builder
         */
        fun add(fieldName: String, value: Int): Builder {
            intent.putExtra(fieldName, value)
            return this
        }

        /**
         * Add extra field data value to intent being built up
         *
         * @param fieldName
         * @param values
         * @return this builder
         */
        fun add(fieldName: String, values: IntArray): Builder {
            intent.putExtra(fieldName, values)
            return this
        }

        /**
         * Add extra field data value to intent being built up
         *
         * @param fieldName
         * @param values
         * @return this builder
         */
        fun add(fieldName: String, values: BooleanArray): Builder {
            intent.putExtra(fieldName, values)
            return this
        }

        /**
         * Add extra field data value to intent being built up
         *
         * @param fieldName
         * @param value
         * @return this builder
         */
        fun add(fieldName: String, value: Serializable): Builder {
            intent.putExtra(fieldName, value)
            return this
        }

        /**
         * Get built intent
         *
         * @return intent
         */
        fun toIntent(): Intent {
            return intent
        }
    }
}