package com.friends.quiz

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * ماسات + متجر سكنات لخانة اسم اللاعب. ملف واحد مستقل لا يعتمد على بقية كود اللعبة.
 *
 *   SkinStore.award(activity, rank)              // عند نهاية الجولة: يضيف الماسات ويعرض نافذة الجائزة (rank = 1 للأول)
 *   SkinStore.openStore(activity, playerName)    // يفتح المتجر
 *   SkinStore.styleNameCard(card, nameTextView)  // يطبّق السكن المختار على خانة اسم اللاعب
 *   SkinStore.diamonds(context)                  // رصيد الماسات
 */
object SkinStore {

    class Skin(
        val id: String, val name: String, val price: Int, val badge: String,
        val colors: IntArray, val stroke: Int, val textColor: Int
    )

    val skins: List<Skin> = listOf(
        Skin("classic", "الكلاسيكي", 0, "👤", intArrayOf(0xFF1F2345.toInt(), 0xFF1F2345.toInt()), 0, 0xFFFFFFFF.toInt()),
        Skin("mint", "نعناع", 25, "🌿", intArrayOf(0xFF00B894.toInt(), 0xFF55EFC4.toInt()), 0, 0xFF0B2E28.toInt()),
        Skin("sky", "سماء", 40, "☁️", intArrayOf(0xFF0984E3.toInt(), 0xFF74B9FF.toInt()), 0, 0xFFFFFFFF.toInt()),
        Skin("pink", "وردة", 60, "🌸", intArrayOf(0xFFE84393.toInt(), 0xFFFD79A8.toInt()), 0, 0xFFFFFFFF.toInt()),
        Skin("sunset", "غروب", 120, "🌅", intArrayOf(0xFFFF7675.toInt(), 0xFFFDCB6E.toInt()), 0, 0xFF3A1A00.toInt()),
        Skin("royal", "ملكي", 200, "👑", intArrayOf(0xFF6C5CE7.toInt(), 0xFFA29BFE.toInt()), 0xFFFFD166.toInt(), 0xFFFFFFFF.toInt()),
        Skin("fire", "نار", 350, "🔥", intArrayOf(0xFFD63031.toInt(), 0xFFFF9F1A.toInt()), 0xFFFFD166.toInt(), 0xFFFFFFFF.toInt()),
        Skin("gold", "ذهبي", 600, "🏆", intArrayOf(0xFFB8860B.toInt(), 0xFFFFD700.toInt(), 0xFFFFF3A0.toInt()), 0xFFFFFFFF.toInt(), 0xFF3A2A00.toInt()),
        Skin("diamond", "ماسي", 1000, "💎", intArrayOf(0xFF00CEC9.toInt(), 0xFF74F2FF.toInt(), 0xFFFFFFFF.toInt()), 0xFF00E5FF.toInt(), 0xFF00323A.toInt()),
        Skin("legend", "أسطوري", 2000, "⭐", intArrayOf(0xFF2D1B69.toInt(), 0xFF8E2DE2.toInt(), 0xFFFF4DA6.toInt()), 0xFFFFD700.toInt(), 0xFFFFFFFF.toInt())
    )

    // ---------- التخزين ----------
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("friends_store", Context.MODE_PRIVATE)

    fun diamonds(ctx: Context): Int {
        val p = prefs(ctx)
        if (!p.contains("diamonds")) p.edit().putInt("diamonds", 50).apply() // هدية البداية
        return p.getInt("diamonds", 0)
    }

    private fun setDiamonds(ctx: Context, v: Int) {
        prefs(ctx).edit().putInt("diamonds", v).apply()
    }

    private fun owned(ctx: Context): Set<String> =
        (prefs(ctx).getString("owned", "classic") ?: "classic").split(",").toSet()

    fun selectedId(ctx: Context): String = prefs(ctx).getString("selected", "classic") ?: "classic"

    fun selectedSkin(ctx: Context): Skin = skins.firstOrNull { it.id == selectedId(ctx) } ?: skins[0]

    private fun select(ctx: Context, id: String) {
        prefs(ctx).edit().putString("selected", id).apply()
    }

    private fun buy(ctx: Context, s: Skin): Boolean {
        val have = diamonds(ctx)
        if (have < s.price) return false
        setDiamonds(ctx, have - s.price)
        prefs(ctx).edit()
            .putString("owned", (owned(ctx) + s.id).joinToString(","))
            .putString("selected", s.id)
            .apply()
        return true
    }

    // ---------- أدوات الرسم ----------
    private fun dp(ctx: Context, v: Int): Int = (v * ctx.resources.displayMetrics.density + 0.5f).toInt()

    private fun round(ctx: Context, color: Int, radiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(ctx, radiusDp).toFloat()
        }

    private fun skinBg(ctx: Context, s: Skin): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, s.colors).apply {
            cornerRadius = dp(ctx, 18).toFloat()
            if (s.stroke != 0) setStroke(dp(ctx, 2), s.stroke)
        }

    /** يطبّق السكن المختار على بطاقة اسم اللاعب (وعلى نصها إن وُجد). */
    fun styleNameCard(card: View, nameView: TextView?) {
        val s = selectedSkin(card.context)
        card.background = skinBg(card.context, s)
        nameView?.setTextColor(s.textColor)
    }

    // ---------- الجوائز ----------
    fun rewardFor(rank: Int): Int = when (rank) {
        1 -> 30
        2 -> 20
        3 -> 10
        else -> 5
    }

    fun award(activity: Activity, rank: Int): Int {
        val d = rewardFor(rank)
        setDiamonds(activity, diamonds(activity) + d)
        showReward(activity, rank, d)
        return d
    }

    private fun showReward(activity: Activity, rank: Int, gained: Int) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val medals = listOf("🥇", "🥈", "🥉")
        val names = listOf("الأول", "الثاني", "الثالث")
        val medal = if (rank in 1..3) medals[rank - 1] else "🎖️"
        val title = if (rank in 1..3) "المركز " + names[rank - 1] + "!" else "المركز " + rank

        val box = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(activity, 28), dp(activity, 28), dp(activity, 28), dp(activity, 20))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xFF2A3262.toInt(), 0xFF1D1B38.toInt())
            ).apply {
                cornerRadius = dp(activity, 24).toFloat()
                setStroke(dp(activity, 2), 0xFFFFD166.toInt())
            }
        }
        fun tv(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(activity).apply {
            text = t; textSize = size; setTextColor(color); gravity = Gravity.CENTER
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
        box.addView(tv(medal, 64f, Color.WHITE))
        box.addView(tv(title, 24f, Color.WHITE, true))
        box.addView(tv("+" + gained + " 💎", 34f, 0xFF7CFFB2.toInt(), true))
        box.addView(tv("رصيدك الآن: " + diamonds(activity) + " 💎", 15f, 0xFF9A9BCB.toInt()))
        val ok = tv("رائع!", 18f, Color.WHITE, true).apply {
            background = round(activity, 0xFF27AE60.toInt(), 16)
            setPadding(dp(activity, 40), dp(activity, 12), dp(activity, 40), dp(activity, 12))
            setOnClickListener { dialog.dismiss() }
        }
        box.addView(ok, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(activity, 18)
        })
        dialog.setContentView(box)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    // ---------- المتجر ----------
    fun openStore(activity: Activity, playerName: String) {
        val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xFF7B1C1F.toInt(), 0xFF1D1B38.toInt())
            )
            setPadding(dp(activity, 16), dp(activity, 28), dp(activity, 16), dp(activity, 12))
        }

        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(activity).apply {
            text = "🛍️ متجر السكنات"; textSize = 24f; setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
        }
        val balance = TextView(activity).apply {
            textSize = 18f; setTextColor(0xFF7CFFB2.toInt()); setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(activity, 10), 0, dp(activity, 14), 0)
        }
        val close = TextView(activity).apply {
            text = "✕"; textSize = 22f; setTextColor(Color.WHITE)
            setPadding(dp(activity, 12), dp(activity, 6), dp(activity, 12), dp(activity, 6))
            setOnClickListener { dialog.dismiss() }
        }
        header.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(balance)
        header.addView(close)
        root.addView(header)

        val sub = TextView(activity).apply {
            text = "اجمع الماسات بالفوز في الجولات، واشترِ سكناً لخانة اسمك"
            textSize = 13f; setTextColor(0xFF9A9BCB.toInt())
            setPadding(0, dp(activity, 4), 0, dp(activity, 12))
        }
        root.addView(sub)

        val list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(activity).apply { addView(list) }
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        fun render() {
            list.removeAllViews()
            balance.text = "💎 " + diamonds(activity)
            val have = diamonds(activity)
            val ownedSet = owned(activity)
            val sel = selectedId(activity)

            for (s in skins) {
                val box = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    background = round(activity, 0xFF2A3262.toInt(), 22)
                    setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12))
                }
                val preview = TextView(activity).apply {
                    text = s.badge + "  " + playerName
                    textSize = 19f
                    setTextColor(s.textColor)
                    setTypeface(typeface, Typeface.BOLD)
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(activity, 18), dp(activity, 16), dp(activity, 18), dp(activity, 16))
                    background = skinBg(activity, s)
                }
                box.addView(preview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

                val tier = when {
                    s.price == 0 -> "مجاني"
                    s.price < 100 -> "رخيص"
                    s.price < 500 -> "متوسط"
                    else -> "فاخر"
                }
                val priceText = if (s.price == 0) "مجاني" else "💎 " + s.price
                val info = TextView(activity).apply {
                    text = s.name + " · " + tier + "\n" + priceText
                    textSize = 15f
                    setTextColor(Color.WHITE)
                }
                val label: String
                val color: Int
                when {
                    s.id == sel -> { label = "مُفعّل ✓"; color = 0xFF4A5380.toInt() }
                    s.id in ownedSet -> { label = "تفعيل"; color = 0xFF2D7FF9.toInt() }
                    have >= s.price -> { label = "شراء"; color = 0xFF27AE60.toInt() }
                    else -> { label = "🔒 ماسات ناقصة"; color = 0xFF555B7A.toInt() }
                }
                val btn = TextView(activity).apply {
                    text = label; textSize = 15f; setTextColor(Color.WHITE)
                    setTypeface(typeface, Typeface.BOLD)
                    gravity = Gravity.CENTER
                    background = round(activity, color, 14)
                    setPadding(dp(activity, 18), dp(activity, 10), dp(activity, 18), dp(activity, 10))
                    setOnClickListener {
                        when {
                            s.id == sel -> {}
                            s.id in ownedSet -> { select(activity, s.id); render() }
                            buy(activity, s) -> {
                                Toast.makeText(activity, "تم شراء " + s.name + " 🎉", Toast.LENGTH_SHORT).show()
                                render()
                            }
                            else -> Toast.makeText(activity, "ماساتك لا تكفي. فز في الجولات لتجمع المزيد 💎", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                val row = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(activity, 10), 0, 0)
                }
                row.addView(info, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                row.addView(btn)
                box.addView(row)

                list.addView(box, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(activity, 12)
                })
            }
        }
        render()
        dialog.setContentView(root)
        dialog.show()
    }
}
