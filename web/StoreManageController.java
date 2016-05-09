package com.ichunshen.dolook.module.product.web;
import java.util.ArrayList;
import java.util.List;
import com.ichunshen.dolook.module.industry.store.model.Menu;
import com.ichunshen.dolook.module.industry.store.model.Store;
import com.ichunshen.dolook.module.industry.store.model.StoreBanner;
import com.ichunshen.dolook.module.user.model.User;
import com.ichunshen.dolook.support.SimpleErrorType;
import com.ichunshen.dolook.utils.CommonUtil;
import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.tx.Tx;
import cn.joy.framework.kits.StringKit;
import cn.joy.framework.rule.RuleResult;
import cn.joy.plugin.jfinal.annotation.Ctrl;

@Ctrl(url = "/storeM")
public class StoreManageController extends Controller {

	public void index() {

	}

	/**
	 * 商店信息
	 * 
	 * @param login_id
	 * @param storeCode
	 */
	public void storeManagement() {
		RuleResult ruleResult = RuleResult.create();
		String storeCode = getPara("storeCode");
		String login_id = getPara("login_id");
		Store storeInfo = new Store();
		if (StringKit.isEmpty(storeCode)) {
			renderText(ruleResult.fail(SimpleErrorType.MISS_PARAMETER).toJSON());
		}
		if (!StringKit.isEmpty(storeCode)) {
			storeInfo = Store.dao.findAdminByCode(storeCode);
		}
		User userInfo = User.dao.findByLoginId(login_id);
		setAttr("storeInfo", storeInfo);
		setAttr("userInfo", userInfo);
		setAttr("storeCode", storeCode);
		renderJsp("store/store_info.jsp");
	}
	
	/**
	 * 商店信息
	 * 
	 * @param login_id
	 * @param storeCode
	 */
	public void editStore() {
		String storeCode = getPara("storeCode");
		setAttr("storeCode", storeCode);
		renderJsp("store/edit_store.jsp");
	}

	/**
	 * 更新商店信息
	 * 
	 * @param login_id
	 * @param storeCode
	 */
	@Before(Tx.class)
	public void updateStoreInfos() {
		RuleResult ruleResult = RuleResult.create();
		String store_name = getPara("store_name");
		String store_code = getPara("storeCode");
		String description = getPara("description");
		String logo = getPara("logo");
		if (StringKit.isEmpty(store_name)) {
			renderText(ruleResult.fail(SimpleErrorType.MISS_PARAMETER).toJSON());
		}
		if (new Store().set("store_name", store_name).set("store_code", store_code).set("description", description)
				.set("logo", logo).update()) {
			ruleResult.success();
		}
		renderJsp("store/store_info.jsp");
	}

	/**
	 * Banner管理 广告位
	 * 
	 * @param storeCode
	 * @param is_mobile
	 */
	public void storeBanner() {
		RuleResult ruleResult = RuleResult.create();
		String store_code = getPara("storeCode");
		String is_mobile = getPara("is_mobile");
//		if (StringKit.isEmpty(store_code) && StringKit.isEmpty(is_mobile)) {
//			renderText(ruleResult.fail(SimpleErrorType.MISS_PARAMETER).toJSON());
//		}
//		String bannerImgs="";
//		StoreBanner banner = StoreBanner.dao.findByCodeAndMob(store_code, is_mobile);
//		List<StoreBanner>  banners = new ArrayList<StoreBanner>();
//		if (banner != null ) {
//				bannerImgs=(CommonUtil.getImageUrl(banner.getStr("store_code"), banner.getStr("banner_img")));
//				banner.put("bannerImgs",bannerImgs);
//				banners.add(banner);
//				setAttr("banner", banners);
//				setAttr("storeCode", store_code);
//				setAttr("is_mobile", is_mobile);
//				renderJsp("store/store_banner_info.jsp");
//		} else{
//			renderText(ruleResult.fail(SimpleErrorType.MISS_RESULT).toJSON());
//		}
		List<StoreBanner>  banners = new ArrayList<StoreBanner>();
		banners=queryStore();
		if(banners == null){
			renderText(ruleResult.fail(SimpleErrorType.MISS_RESULT).toJSON());
		} else{
			setAttr("banner", banners);
			setAttr("storeCode", store_code);
			setAttr("is_mobile", is_mobile);
			renderJsp("store/store_banner_info.jsp");
		}
		
	}

	/**
	 * Banner保存 广告位
	 * 
	 * @param storeCode
	 * @param is_mobile
	 * @param banner_url
	 */
	@Before(Tx.class)
	public void saveStoreBanner() {
		RuleResult ruleResult = RuleResult.create();
		String store_code = getPara("storeCode");
		String is_mobile = getPara("is_mobile");
		String id = getPara("id");
		String[] banner_url = getParaValues("bannerUrl");
		String[] banner_img = getParaValues("bannerImg");
		if (StringKit.isEmpty(banner_url) || StringKit.isEmpty(store_code)) {
			renderText(ruleResult.fail(SimpleErrorType.MISS_PARAMETER).toJSON());
		}
		for(int i=0;i<banner_url.length;i++){
			new StoreBanner().set("store_code", store_code).set("is_mobile", is_mobile).set("banner_url", banner_url[i])
			.set("banner_img", banner_img[i]).save();
		}
		
		setAttr("storeCode", store_code);
		renderJsp("store/store_banner_info.jsp");
	}

	/**
	 * seo搜索
	 * 
	 * @param storeCode
	 */
	public void updateSeo() {
		RuleResult ruleResult = RuleResult.create();
		String store_code = getPara("storeCode");
		String seo_description = getPara("seo_description");
		String seo_keywords = getPara("seo_keywords");
		String seo_title = getPara("seo_title");
		if (StringKit.isEmpty(store_code)) {
			renderText(ruleResult.fail(SimpleErrorType.MISS_PARAMETER).toJSON());
		}
		if(StringKit.isEmpty(seo_keywords)&& StringKit.isEmpty(seo_description)){
			setAttr("storeCode", store_code);
			renderJsp("store/store_seo.jsp");
			return;
		}
		Store store=Store.dao.findByCode(store_code);
		if(store == null){
			renderText(ruleResult.fail(SimpleErrorType.MISS_RESULT).toJSON());
		}
		store.set("seo_keywords", seo_keywords).set("seo_description", seo_description).set("seo_title", seo_title).update();
		setAttr("storeCode", store_code);
		renderJsp("store/store_seo.jsp");
		
	}
	
	/**
	 * seo搜索
	 * 
	 * @param storeCode
	 */
	public void quuerySeo() {
		RuleResult ruleResult = RuleResult.create();
		String store_code = getPara("storeCode");
		Store store=Store.dao.findByCode(store_code);
		if(store == null){
			renderText(ruleResult.fail(SimpleErrorType.MISS_RESULT).toJSON());
		}
		
//s		store.set("seo_keywords", 123).set("seo_description", 222).set("seo_description", 55);
		setAttr("storeCode", store_code);
		setAttr("store", store);
		renderJsp("store/store_seo.jsp");
		
	}


	/**
	 * 微店菜單
	 * 
	 * @param storeCode
	 */
	public void menuInfo() {
		RuleResult ruleResult = RuleResult.create();
		String store_code = getPara("storeCode");
		String menu_Name = getPara("menuName");
		String level = getPara("level");
		//分頁
		int pageSize = 10;
		long num = Menu.dao.countMenu(store_code);
		int pageNum = getPageNum(num, pageSize);
		int nextPage = 1;

		int page = getParaToInt("p", 1);
		if (page < 0) {
			page = 1;
		}
		nextPage = page + 1;
		if (page > pageNum) {
			page = pageNum;
		}
		if (nextPage > pageNum) {
			nextPage = pageNum;
		}
		int sn = (page - 1) < 0 ? 0 : (page - 1) * pageSize;
		//分頁
		if (StringKit.isEmpty(store_code)) {
			renderText(ruleResult.fail(SimpleErrorType.MISS_PARAMETER).toJSON());
		}
		List<Menu> menuInfo = Menu.dao.findByStoreMenu(store_code ,menu_Name ,level,sn,pageSize);
		if (menuInfo != null && menuInfo.size() > 0) {
			for (Menu meun : menuInfo) {
				String image=CommonUtil.getImageUrl(meun.getStr("store_code"), meun.getStr("image"));
						meun.put("meunImage",image);
			}
		}
		setAttr("nextPage", nextPage);
		setAttr("p", page);
		setAttr("pageNum", pageNum);
		setAttr("level", level);
		setAttr("menuName", menu_Name);
		setAttr("menuInfo", menuInfo);
		setAttr("storeCode", store_code);
		renderJsp("store/store_menu.jsp");
	}

	/**
	 * 微店菜單新增
	 * 
	 * @param storeCode
	 */
	public void addMenuInfo() {
		RuleResult ruleResult = RuleResult.create();
		String store_code = getPara("storeCode");
		String menu_name = getPara("menuName");
		String menu_type = getPara("menuType");
		String url = getPara("url");
		if (StringKit.isEmpty(menu_name) || StringKit.isEmpty(menu_type)) {
			renderText(ruleResult.fail(SimpleErrorType.MISS_PARAMETER).toJSON());
		}
		new Menu().set("store_code", store_code).set("menu_name", menu_name).set("menu_type", menu_type).set("url", url)
				.save();
		setAttr("storeCode", store_code);
		renderJsp("store/store_menu.jsp");
	}
	
	/**
	 * 模板管理
	 * @param storeCode
	 * @param mall_template_code_pc
	 * @param mall_template_code_mobile
	 * @param mall_template_code
	 */
	public void mallTemplate(){
		String store_code = getPara("store_code");
		String code_pc = getPara("mall_template_code_pc");
		String code_mobile = getPara("mall_template_code_mobile");
		//分頁
				int pageSize = 10;
				long num = Menu.dao.countMenu(store_code);
				int pageNum = getPageNum(num, pageSize);
				int nextPage = 1;

				int page = getParaToInt("p", 1);
				if (page < 0) {
					page = 1;
				}
				nextPage = page + 1;
				if (page > pageNum) {
					page = pageNum;
				}
				if (nextPage > pageNum) {
					nextPage = pageNum;
				}
				int sn = (page - 1) < 0 ? 0 : (page - 1) * pageSize;
				//分頁
		setAttr("storeCode", store_code);
		setAttr("mall_template_code_pc", code_pc);
		setAttr("mall_template_code_mobile", code_mobile);
		renderJsp("store/mall_template.jsp");
	}
	
	
	/**
	 * 导航管理新增
	 * @param storeCode
	 */
	public void transfer() {
		String store_code = getPara("storeCode");
		String is_mobile = getPara("is_mobile");
		
		List<StoreBanner>  banners = new ArrayList<StoreBanner>();
		banners=queryStore();
		if(banners !=null){
			setAttr("banner", banners);
		} 
		setAttr("storeCode", store_code);
		setAttr("is_mobile", is_mobile);
		renderJsp("store/add_banner.jsp");
	}
	
	
	private int getPageNum(long num, int size) {
		if (num <= 0) {
			return 1;
		}
		int n = (int) num / size;
		if (num % size == 0) {
			return n;
		}
		return n + 1;

	}
	
	private List<StoreBanner>  queryStore(){
		RuleResult ruleResult = RuleResult.create();
		String store_code = getPara("storeCode");
		String is_mobile = getPara("is_mobile");
		if (StringKit.isEmpty(store_code) && StringKit.isEmpty(is_mobile)) {
			renderText(ruleResult.fail(SimpleErrorType.MISS_PARAMETER).toJSON());
		}
		String bannerImgs="";
		StoreBanner banner = StoreBanner.dao.findByCodeAndMob(store_code, is_mobile);
		List<StoreBanner>  banners = new ArrayList<StoreBanner>();
		if (banner != null ) {
				bannerImgs=(CommonUtil.getImageUrl(banner.getStr("store_code"), banner.getStr("banner_img")));
				banner.put("bannerImgs",bannerImgs);
				banners.add(banner);
				return banners;
		}
		return null;
	}

}
