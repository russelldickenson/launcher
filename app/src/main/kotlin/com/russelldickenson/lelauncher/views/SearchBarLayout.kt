package com.russelldickenson.lelauncher.views

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doAfterTextChanged
import com.russelldickenson.lelauncher.R
import com.russelldickenson.lelauncher.databinding.ViewSearchBarBinding
import com.russelldickenson.lelauncher.extensions.adjustAlpha
import com.russelldickenson.lelauncher.extensions.getContrastColor
import com.russelldickenson.lelauncher.extensions.getThemeColor

class SearchBarLayout(context: Context, attrs: AttributeSet) : InsetAppBarLayout(context, attrs) {
    var isSearchOpen = false
        private set
    var onSearchTextChangedListener: ((text: String) -> Unit)? = null

    val binding = ViewSearchBarBinding.inflate(LayoutInflater.from(context), this)

    fun setupMenu() {
        binding.searchBarIcon.setOnClickListener {
            if (isSearchOpen) {
                closeSearch()
            } else {
                binding.searchBarInput.requestFocus()
                showKeyboard()
            }
        }

        post {
            binding.searchBarInput.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    openSearch()
                }
            }
        }

        binding.searchBarInput.doAfterTextChanged { onSearchTextChangedListener?.invoke(it?.toString().orEmpty()) }
    }

    private fun openSearch() {
        isSearchOpen = true
        binding.searchBarIcon.setImageResource(R.drawable.ic_arrow_left_vector)
        binding.searchBarIcon.contentDescription = resources.getString(R.string.back)
    }

    fun closeSearch() {
        isSearchOpen = false
        binding.searchBarInput.setText("")
        binding.searchBarIcon.setImageResource(R.drawable.ic_search_vector)
        binding.searchBarIcon.contentDescription = resources.getString(R.string.search)
        hideKeyboard()
    }

    fun getCurrentQuery() = binding.searchBarInput.text.toString()

    fun updateColors() {
        val backgroundColor = context.getThemeColor(android.R.attr.colorBackground)
        val contrastColor = backgroundColor.getContrastColor()
        setBackgroundColor(backgroundColor)
        binding.searchBarIcon.setColorFilter(contrastColor)
        binding.searchBarInput.setTextColor(contrastColor)
        binding.searchBarInput.setHintTextColor(contrastColor.adjustAlpha(MEDIUM_ALPHA))
        binding.searchBarContainer.background.mutate().alpha = (LOWER_ALPHA * 255).toInt()
    }

    private fun showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.searchBarInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow((context as? Activity)?.currentFocus?.windowToken ?: windowToken, 0)
    }

    private companion object {
        const val LOWER_ALPHA = 0.25f
        const val MEDIUM_ALPHA = 0.5f
    }
}
