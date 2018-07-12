package tti.com.ttidlnacaster.base

/**
 * Created by dylan_liang on 2018/2/9.
 */
interface BasePresenter<T> {
    fun bindView(view: T)
}
