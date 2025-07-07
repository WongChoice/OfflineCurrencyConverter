package com.face.Firstcal

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CurrencySelectorDialog(
    private val allCurrencies: List<String>,
    private val selectedCurrencies: List<String>,
    private val onSelectionConfirmed: (List<String>) -> Unit
) : BottomSheetDialogFragment() {

    private val currentSelection = selectedCurrencies.toMutableSet()
    private var filteredCurrencies = allCurrencies.toList()

    private lateinit var listView: ListView
    private lateinit var searchEditText: EditText
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.Theme_Design_BottomSheetDialog)
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            val params = (it.window?.attributes)
            params?.height = ViewGroup.LayoutParams.MATCH_PARENT
            it.window?.attributes = params as WindowManager.LayoutParams
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.dialog_currency_selector, container, false)

        searchEditText = rootView.findViewById(R.id.searchEditText)
        listView = rootView.findViewById(R.id.currencyListView)
        val confirmButton = rootView.findViewById<Button>(R.id.confirmButton)
        val cancelButton = rootView.findViewById<Button>(R.id.cancelButton)

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_multiple_choice, filteredCurrencies)
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        listView.adapter = adapter

        // Preselect
        allCurrencies.forEachIndexed { index, currency ->
            if (selectedCurrencies.contains(currency)) {
                listView.setItemChecked(index, true)
            }
        }

        // Search listener
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                filteredCurrencies = allCurrencies.filter { it.lowercase().contains(query) }
                adapter.clear()
                adapter.addAll(filteredCurrencies)
                adapter.notifyDataSetChanged()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            val currency = filteredCurrencies[position]
            if (currentSelection.contains(currency)) {
                currentSelection.remove(currency)
            } else {
                if (currentSelection.size < 6) {
                    currentSelection.add(currency)
                } else {
                    Toast.makeText(requireContext(), "Max 6 currencies", Toast.LENGTH_SHORT).show()
                    listView.setItemChecked(position, false)
                }
            }
        }

        confirmButton.setOnClickListener {
            onSelectionConfirmed(currentSelection.toList())
            dismiss()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }

        return rootView
    }
}
