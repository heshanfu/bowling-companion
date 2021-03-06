package ca.josephroque.bowlingcompanion.common

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

/**
 * Copyright (C) 2018 Joseph Roque
 *
 * Adds thousands separators to [EditText].
 */
open class ThousandsTextWatcher(
    private val groupingSeparator: String,
    private val decimalSeparator: String
) : TextWatcher {

    private var isBusy = false

    override fun afterTextChanged(s: Editable?) {
        if (s != null && !isBusy) {
            isBusy = true

            var place = 0

            val decimalPointIndex = s.indexOf(decimalSeparator)
            var i = if (decimalPointIndex == -1) {
                s.length - 1
            } else {
                decimalPointIndex - 1
            }
            while (i >= 0) {
                val c = s[i]
                if (c == ',') {
                    s.delete(i, i + 1)
                } else {
                    if (place % 3 == 0 && place != 0) {
                        // insert a comma to the left of every 3rd digit (counting from right to
                        // left) unless it's the leftmost digit
                        s.insert(i + 1, groupingSeparator)
                    }
                    place++
                }
                i--
            }

            isBusy = false
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
}
