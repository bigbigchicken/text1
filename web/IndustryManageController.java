package com.ichunshen.dolook.module.product.web;

import java.util.ArrayList;
import java.util.List;

import cn.joy.framework.kits.StringKit;
import cn.joy.framework.rule.RuleResult;
import cn.joy.plugin.jfinal.annotation.Ctrl;

import com.ichunshen.dolook.module.industry.store.model.Store;
import com.ichunshen.dolook.module.user.model.User;
import com.ichunshen.dolook.support.SimpleErrorType;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Page;

@Ctrl(url = "industrymanage")
public class IndustryManageController extends Controller {
	/**
	 * 店铺列表
	 */
	public void index() {
		String storeCode = getPara("storeCode", "");
		int pageNum = getParaToInt("p", 1);
		int pageSize = 10;
		Store store = Store.dao.findByCode(storeCode);
		if(StringKit.isEmpty(storeCode) || store == null){
			redirect("/productManage?storeCode="+storeCode);
			return;
		}
		List<Store> stores = new ArrayList<Store>();
		if(store.getStoreCode().equals(store.getIndustryCode())){
			//行业管理员
			Page<Store> result = Store.dao.paginateByByIndustryCode(pageNum, pageSize, store.getIndustryCode());
			stores.addAll(result.getList());
			setAttr("curPage", result.getPageNumber());
			setAttr("totalPage", result.getTotalPage());
		}else{
			setAttr("curPage", 1);
			setAttr("totalPage", 1);
			store.put("username", User.dao.findByLoginId(store.getStr("founder_login_id")).getUsername());
			stores.add(store);
		}
		setAttr("storeInfos",stores);
		setAttr("storeCode",storeCode);
		renderJsp("store/industry_manage.jsp");

	}
	/**
	 * 店铺列表
	 */
	public void list() {
		RuleResult ruleResult = RuleResult.create();
		String industryCode = getPara("industryCode");
		String storeCode = getPara("storeCode");
		storeCode = "70197";
		industryCode="97143";
		String login_id = getPara("login_id");
		login_id = "uyry6o9rmm";
		List<Store> stores = new ArrayList<Store>();
		if (StringKit.isEmpty(storeCode) || StringKit.isEmpty(industryCode)) {
			renderText(ruleResult.fail(SimpleErrorType.MISS_PARAMETER).toJSON());
		}
		if (!StringKit.isEmpty(storeCode)) {
				
		} else if (!StringKit.isEmpty(industryCode)) {
			Store store = (Store) Store.dao.listStoreMe(industryCode,login_id);
			User userInfo = User.dao.findByLoginId(login_id);
			store.put("userInfo",userInfo);
			stores.add(store);			
		}
		setAttr("storeInfos",stores);
		setAttr("storeCode",storeCode);
		renderJsp("store/industry_manage.jsp");

	}
	
}