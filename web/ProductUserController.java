package com.ichunshen.dolook.module.product.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.ichunshen.dolook.module.industry.member.web.RecommendController;
import com.ichunshen.dolook.module.industry.model.Industry;
import com.ichunshen.dolook.module.industry.store.model.Store;
import com.ichunshen.dolook.module.industry.store.model.StoreUserRel;
import com.ichunshen.dolook.module.user.model.User;
import com.ichunshen.dolook.module.user.web.UserController;
import com.ichunshen.dolook.support.DoLookConstant;
import com.ichunshen.dolook.support.SimpleErrorType;
import com.ichunshen.dolook.support.UserLoginUtil;
import com.ichunshen.dolook.temp.DoLookCache;
import com.ichunshen.dolook.temp.bean.UserBean;
import com.ichunshen.dolook.temp.logic.StoreLogic;
import com.ichunshen.dolook.utils.CommonUtil;
import com.ichunshen.dolook.utils.WebUtils;
import com.jfinal.aop.Clear;
import com.jfinal.core.Controller;
import com.mysql.fabric.xmlrpc.base.Array;

import cn.joy.framework.kits.EncryptKit;
import cn.joy.framework.kits.StringKit;
import cn.joy.framework.rule.RuleParam;
import cn.joy.framework.rule.RuleResult;
import cn.joy.plugin.jfinal.annotation.Ctrl;


@Ctrl(url="/productUser")
public class ProductUserController extends Controller{
	
	public static Logger logger = Logger.getLogger(ProductUserController.class);
	
	

	public void index(){
		renderJsp("product_manage/product_login.jsp");
	}
	
	
	
	/**
	 * 登录校验
	 */
	@Clear
	public void doLogin() {
		RuleResult result = RuleResult.create();
		
		String username = getPara("username", "").trim();
		String pwd = getPara("pwd", "").trim();
		if(StringKit.isNotEmpty(pwd)){
			pwd = EncryptKit.md5(pwd);
		}	
	//StoreUserRel storeUserRel = StoreUserRel.dao.findMyUserSotreByLoginId(username);
	
		logger.info("username:" + username + ";pwd:" + pwd);

		if (StringKit.isEmpty(username) || (StringKit.isEmpty(pwd) )) {
			renderText(result.fail(SimpleErrorType.MISS_PARAMETER).toJSON());
			renderJsp("");
			return;
			
		}

		result = WebUtils.invokeWebsiteOpenRule(username, "account.accountService#loginFromWeb",
				new RuleParam().put("user", username).put("pwd", pwd)
				.put("industryCode", this.getSessionAttr(DoLookConstant.INDUSTRY_CODE)));
		logger.info("checkUser-result==" + result.toString());

		if (result.isSuccess()) {
			Map<String, Object> content = result.getMapFromContent();
			
			//登录成功回调处理
			//1 保存或更新电商用户信息
			User user = UserLoginUtil.saveOrUpdateUser((String)content.get("loginId"));
			//2 用户放入session，缓存，写入cookie等
			UserLoginUtil.loadWebUser(this.getRequest(), this.getResponse(), user, (String) content.get("tn"));
			
			/*
			 * chenjs modified
			 * 注册后登录初始化推荐人信息
			 */
			
			if(StringKit.isEmpty(user.getStr("from_login_id"))){
				this.getRequest().setAttribute("source", (String) content.get("fromUser"));
			}
			this.getRequest().setAttribute("storeCode", CommonUtil.getStoreCodeForDlook(getRequest(), getResponse()));
			RecommendController.initRecommender(DoLookCache.getUserFromSession(this.getRequest()), this.getRequest(), this.getResponse());
			
			redirect("/productUser/getUser");
	
		}else{
			setAttr("msg", "用户名或密码错误");
			renderJsp("product_manage/product_login.jsp");
		}
		
	}
	
	
	/**
	 * 防止微信 openid
	 * 
	 * @param openId
	 *//*
	private void initSessionWXOpenID(String openId) {
		if (StringKit.isNotEmpty(openId)) {
			setAttr("openid", openId);
			setSessionAttr("openid", openId);
		}
	}*/
	
	/**
	 * 判断管理员类型
	 * 
	 */
	public void  getUser(){
		UserBean user = DoLookCache.getUserFromSession(getRequest());
		if(user == null){
			setAttr("msg", "请先登录");
			renderJsp("product_manage/product_login.jsp");
			return;
		}
		
		String loginId="uns2k51l3b";
		
		List<StoreUserRel> users=StoreUserRel.dao.findUserInfos(loginId);
		List<Store> stores= new ArrayList<Store>();
		List<Store> industrys=new ArrayList<Store>();
		for(StoreUserRel temp:users){
			String store_code=temp.getStr("store_code");
				Store store = Store.dao.findAdminByCode(store_code);
				if(store !=null){
					if(!store.get("store_code").equals(store.get("industry_code"))){
						stores.add(store);
					} else{
						industrys.add(store);
					}
				} 
		}
		setAttr("stores",stores);
		setAttr("industrys",industrys);
		renderJsp("product_manage/product_storeinfor.jsp");
	}

}
