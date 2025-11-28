package com.example.deokmoa.ui

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.deokmoa.AddReviewActivity
import com.example.deokmoa.R
import com.example.deokmoa.data.AppDatabase
import com.example.deokmoa.data.CategoryEntity
import com.example.deokmoa.databinding.FragmentSettingBinding
import kotlinx.coroutines.launch

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()

        binding.btnManageCategory.setOnClickListener {
            showCategoryDialog()
        }
    }

    private fun showCategoryDialog() {
        val context = requireContext()

        // 1. 다이얼로그 전체 틀
        val dialogLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
            setBackgroundColor(Color.WHITE)
        }

        // 2. 제목
        val tvTitle = TextView(context).apply {
            text = "카테고리 목록"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(16) }
        }
        dialogLayout.addView(tvTitle)

        // 3. 리스트
        val rvCategoryList = RecyclerView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(200)
            ).apply { bottomMargin = dpToPx(16) }
        }
        dialogLayout.addView(rvCategoryList)

        // 4. 추가 버튼
        val btnAdd = Button(context).apply {
            text = "+ 카테고리 추가"
            setBackgroundColor(ContextCompat.getColor(context, R.color.main))
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        dialogLayout.addView(btnAdd)

        // 5. 닫기 버튼
        val btnClose = Button(context).apply {
            text = "닫기"
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.DKGRAY)
            stateListAnimator = null
        }
        dialogLayout.addView(btnClose)

        // 6. 다이얼로그 생성 및 표시
        val builder = AlertDialog.Builder(context).setView(dialogLayout)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        // DB에서 카테고리 로드
        val database = AppDatabase.getDatabase(requireContext())
        val categoryList = mutableListOf<CategoryEntity>()
        val adapter = CategoryManageAdapter(categoryList)
        rvCategoryList.layoutManager = LinearLayoutManager(context)
        rvCategoryList.adapter = adapter

        // LiveData 관찰
        database.categoryDao().getAll().observe(viewLifecycleOwner) { categories ->
            categoryList.clear()
            categoryList.addAll(categories ?: emptyList())
            adapter.notifyDataSetChanged()
        }

        // 7. 추가 버튼 클릭 시 입력 다이얼로그 띄우기
        btnAdd.setOnClickListener {
            val input = EditText(context).apply {
                hint = "카테고리 이름을 입력하세요"
            }

            val container = FrameLayout(context)
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(24)
                rightMargin = dpToPx(24)
            }
            input.layoutParams = params
            container.addView(input)

            AlertDialog.Builder(context)
                .setTitle("카테고리 추가")
                .setView(container)
                .setPositiveButton("확인") { _, _ ->
                    val newName = input.text.toString().trim()
                    if (newName.isNotEmpty()) {
                        lifecycleScope.launch {
                            val newCategory = CategoryEntity(name = newName, isDefault = false)
                            database.categoryDao().insert(newCategory)
                            Toast.makeText(context, "'$newName' 카테고리가 추가되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                       Toast.makeText(context, "이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("취소", null)
                .show()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    inner class CategoryManageAdapter(private val items: MutableList<CategoryEntity>) :
        RecyclerView.Adapter<CategoryManageAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val layout = view as LinearLayout
            val tvName: TextView = layout.getChildAt(0) as TextView
            val btnDelete: TextView = layout.getChildAt(1) as TextView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val context = parent.context

            val itemLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12))
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                val outValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
            }

            val tvName = TextView(context).apply {
                textSize = 16f
                setTextColor(Color.BLACK)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }
            itemLayout.addView(tvName)

            val btnDelete = TextView(context).apply {
                text = "-"
                textSize = 20f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.RED)
                setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4))
            }
            itemLayout.addView(btnDelete)

            return ViewHolder(itemLayout)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val category = items[position]
            holder.tvName.text = category.name

            // 기본 카테고리면 삭제 버튼 숨김
            if (category.isDefault) {
                holder.btnDelete.visibility = View.INVISIBLE
            } else {
                holder.btnDelete.visibility = View.VISIBLE
            }

            holder.btnDelete.setOnClickListener {
                val categoryToDelete = items[position]
                
                // 삭제 전 검증
                lifecycleScope.launch {
                    val database = AppDatabase.getDatabase(requireContext())
                    val reviewCount = database.reviewDao().countReviewsByCategory(categoryToDelete.name)
                    
                    if (reviewCount > 0) {
                        Toast.makeText(
                            requireContext(),
                            "'${categoryToDelete.name}' 카테고리를 사용 중인 리뷰가 ${reviewCount}개 있습니다. 삭제할 수 없습니다.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        database.categoryDao().delete(categoryToDelete)
                        Toast.makeText(
                            requireContext(),
                            "'${categoryToDelete.name}' 카테고리가 삭제되었습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.main_toolbar, menu)
                menu.findItem(R.id.action_add)?.isVisible = true
                menu.findItem(R.id.action_search)?.isVisible = false
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add -> {
                        val intent = android.content.Intent(requireContext(), AddReviewActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}