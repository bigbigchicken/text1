package com.ichunshen.dolook.module.product.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import cn.joy.framework.kits.JsonKit;
import cn.joy.framework.kits.StringKit;
import cn.joy.framework.rule.RuleResult;
import cn.joy.framework.server.RouteManager;
import cn.joy.plugin.jfinal.annotation.Ctrl;
import com.ichunshen.dolook.module.industry.store.model.Store;
import com.ichunshen.dolook.module.product.model.Postage;
import com.ichunshen.dolook.module.product.model.Product;
import com.ichunshen.dolook.module.product.model.ProductCatagory;
import com.ichunshen.dolook.module.product.model.ProductImage;
import com.ichunshen.dolook.module.product.model.ProductNum;
import com.ichunshen.dolook.module.product.model.ProductPostage;
import com.ichunshen.dolook.module.product.model.ProductProperty;
import com.ichunshen.dolook.module.product.model.ProductPropertyValue;
import com.ichunshen.dolook.module.product.model.ProductSku;
import com.ichunshen.dolook.module.product.model.ProductTProperty;
import com.ichunshen.dolook.module.product.model.ProductTPropertyValue;
import com.ichunshen.dolook.module.product.support.ProductErrorType;
import com.ichunshen.dolook.support.PathUtil;
import com.ichunshen.dolook.support.SimpleErrorType;
import com.ichunshen.dolook.temp.DoLookCache;
import com.ichunshen.dolook.temp.bean.UserBean;
import com.ichunshen.dolook.utils.CommonUtil;
import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.tx.Tx;

@Ctrl(url = "/productManage")
public class ProductManageController extends Controller {
	/**
	 * 产品邮费模板，分类
	 */
	public void index() {
		String storeCode = getPara("storeCode");
		setAttr("storeCode", storeCode);
		renderJsp("store/index.jsp");
	}
	
	/**
	 * 跳转发布商品页面
	 * @param storeCode
	 */
	public void publicProduct(){
		String storeCode = getPara("storeCode");
		setAttr("storeCode", storeCode);
		Store store = Store.dao.findByCode(storeCode);
		if(StringKit.isEmpty(storeCode) || store == null){
			redirect("/productManage?storeCode="+storeCode);
			return;
		}
		List<Postage> postages = Postage.dao.listPostsByScode(storeCode);
		setAttr("postages", postages);

		String industryCode = store.getIndustryCode();
		List<ProductCatagory> catagories = ProductCatagory.dao.listProductCatagory(industryCode);

		setAttr("catagories", catagories);
		renderJsp("product_manage/product_publish.jsp");
	}

	/**
	 * 產品屬性
	 */
	public void getProperty() {
		RuleResult ruleResult = RuleResult.create();
		String cate_id = getPara("cate_id");
		if (StringKit.isEmpty(cate_id)) {
			renderText(ruleResult.fail(ProductErrorType.PRODUCT_CATEGORY_EMPTY, "cate_id").toJSON());
			return;
		}
		List<ProductProperty> pps = ProductProperty.dao.findByCategory(cate_id);
		boolean isColorFlag = true;
		ProductProperty colorObj = null;
		List<ProductProperty> removeProp = new ArrayList<>();
		for (ProductProperty pp : pps) {
			// 获取属性值
			List<ProductPropertyValue> propertyValues = ProductPropertyValue.dao
					.findByCategory(pp.getLong("id").toString());
			if (propertyValues != null) {
				pp.put("values", propertyValues);
				// 判断颜色
				if (isColorFlag && "颜色".equalsIgnoreCase(pp.getStr("p_name"))) {
					pp.put("isColor", true);
					colorObj = pp;
					removeProp.add(pp);
					isColorFlag = false;
				} else {
					pp.put("isColor", false);
				}
			} else {
				removeProp.add(pp);
			}
		}
		pps.removeAll(removeProp);
		if(colorObj != null)
			pps.add(0, colorObj);

		ruleResult.success();
		renderText(ruleResult.putContent(pps).toJSON());
		return;

	}

	@Before(Tx.class)
	public void saveProduct() {

		RuleResult ruleResult = RuleResult.create();

		UserBean user = DoLookCache.getUserFromSession(getRequest());
		if(user == null){
			HttpServletRequest req = getRequest();
			req.getSession().setAttribute("backUrl", PathUtil.getRequestUrl(req));
			renderText(ruleResult.fail(SimpleErrorType.NEED_USER_LOGIN).toJSON());
			return;
		}

		String pcode = CommonUtil.generateCode("CP");
		String resultJson = getPara("result").trim();
		if (resultJson == null) {
			renderText(ruleResult.fail(SimpleErrorType.MISS_PARAMETER).toJSON());
			return;
		}
		Map<String, Object> result = JsonKit.json2Map(resultJson);

		String storeCode = String.valueOf(result.get("storeCode"));
		if(StringUtils.isEmpty(storeCode)){
			renderText(ruleResult.fail(SimpleErrorType.NEED_STORE_CODE).toJSON());
			return;
		}
		Store store = Store.dao.findByCode(storeCode);
		if(store == null){
			renderText(ruleResult.fail(SimpleErrorType.NEED_STORE_CODE).toJSON());
			return;
		}
		String industryCode = store.getIndustryCode();
		
		String pname = String.valueOf(result.get("pname"));
		String intro = String.valueOf(result.get("intro"));
		String quantity = String.valueOf(result.get("quantity"));
		String price = String.valueOf(result.get("price")); //
		String cost_price_p = String.valueOf(result.get("cost_price")); //
		String cate_id = String.valueOf(result.get("cate_id"));
		String postage_id = String.valueOf(result.get("postage_id"));
		List<Map<String, String>> skuList = (List<Map<String, String>>) result.get("SKU");
		String type = String.valueOf(result.get("type"));
		List<String> product_image = (List<String>) result.get("product_image");
		List<Map<String, String>> color_images = (List<Map<String, String>>) result.get("color_images");
		List<Map<String, String>> change = (List<Map<String, String>>) result.get("modify");

		if (StringUtils.isEmpty(pname) || StringUtils.isEmpty(intro) || StringUtils.isEmpty(quantity)
				|| StringUtils.isEmpty(price) || StringUtils.isEmpty(cate_id) || user == null) {
			renderText(ruleResult.fail(SimpleErrorType.MISS_PARAMETER).toJSON());
			return;
		}

		String imgServerUrl = RouteManager.getServerURLByCompanyCode("file", storeCode) + "/uploadImage/";
		// 产品表
		Product p = new Product().set("pcode", pcode).set("pname", pname).set("intro", intro).set("quantity", quantity)
				.set("price", price).set("orgin_price", price).set("login_id", user.getLoginId())
				.set("modify_time", new Date()).set("p_status", 0).set("cost_price", cost_price_p).set("cate_id", cate_id)
				.set("image", product_image == null || product_image.size() == 0 ? null : imgServerUrl + product_image.get(0))
				.set("creat_time", new Date()).set("postage_id", postage_id).set("store_code", storeCode)
				.set("p_type", type).set("industry_code", industryCode);
		p.save();

		Long productId = p.getId();
		// 产品计数表
		new ProductNum().set("pcode", pcode).save();
		// pM.save();
		// sku{ } price{} 库存{}
		Set<String> p_v = new HashSet<String>();
		Set<String> pid = new HashSet<String>();
		Map<String, String> propIds = changeColorName(skuList,change);
		if (skuList != null) {
			for (int i = 0; i < skuList.size(); i++) {
				Map<String, String> product = skuList.get(i);
				String sku = product.get("sku");
				String[] skus = sku.split(";");
				// 过滤重复属性
				for (int j = 0; j < skus.length; j++) {
					p_v.add(skus[j]);
				}
				// 保存SKU
				new ProductSku().set("pcode", pcode).set("properties", sku.substring(0, sku.length() - 1)).set("delflag", 0)
						.set("productid", productId).set("quantity", product.get("quantity"))
						.set("price", product.get("price")).set("creat_time", new Date())
						.set("proplabel", product.get("proplabel")).save();

			}
		}

		// 商品图片
		for (int i=1; i<product_image.size(); i++) {
			String image_url = product_image.get(i);
			if(StringUtils.isEmpty(image_url))	continue;
			new ProductImage().set("pcode", pcode).set("image", imgServerUrl + image_url).set("lastmodifytime", new Date())
					.set("productid", productId).save();
		}

		// 颜色图片
		if (color_images != null) {
			for (Map<String, String> images : color_images) {
				if(StringUtils.isNotEmpty(images.get("url"))){
					new ProductImage().set("pcode", pcode).set("sku_pro", propIds.get(images.get("id"))).set("image", imgServerUrl + images.get("url"))
						.set("lastmodifytime", new Date()).set("productid", productId).save();
				}
			}
		}

		// 属性值关系
		for (String s : p_v) {
			String[] ss = s.split(":");
			for (int j = 0; j < ss.length; j++) {
				pid.add(ss[0]);
			}
			new ProductTPropertyValue().set("store_code", storeCode).set("pid", ss[0]).set("vid", ss[1])
					.set("pcode", pcode).set("productid", productId).set("lastmodifytime", new Date()).save();
		}

		// 保存商品属性的对应关系
		for (String s : pid) {
			new ProductTProperty().set("pid", s).set("store_code", storeCode).set("pcode", pcode).save();
		}

		// 保存邮费
		new ProductPostage().set("postage_id", postage_id).set("pcode", pcode).set("productid", productId).save();
		ruleResult.success();
		renderText(ruleResult.toJSON());
		return;
	}
	
	private Map<String, String> changeColorName(List<Map<String, String>> oSkuList, List<Map<String, String>> change){
		
		Map<String, String> propIds = new HashMap<>();
		
		if (change!=null) {// 判断修改的颜色是不是为空
			for (Map<String, String> modify : change) {
				Long new_id = null;//新颜色的id
				String s_pid = modify.get("pid");
				String original_id = modify.get("id");
				String str = modify.get("m");
				//到旧sku中与compare变量匹配，匹配成功则说明有此颜色，没有匹配则说明此颜色未勾选
				String compare = new StringBuilder().append(s_pid).append(":").append(original_id).toString();
				// 对修改过颜色名字，但未选中的信息过滤
				for (Map<String, String> skuList_single : oSkuList) {
					String sku = skuList_single.get("sku");
					if (sku.contains(compare)) {
						// save,返回id
						ProductPropertyValue ppv = new ProductPropertyValue().set("pid", s_pid).set("pvalue", str)
								.set("del_flag", 0).set("image", modify.get("image"));
						ppv.save();
						new_id = ppv.get("id");
						propIds.put(original_id, String.valueOf(new_id));
						break;
					}
				}
				// 将新的颜色id赋值给修改过名字的颜色
				for (Map<String, String> sku_item : oSkuList) {
					String skuStr = sku_item.get("sku");
					sku_item.put("sku", skuStr.replace(":" + original_id + ";", ":" + new_id + ";"));
				}
			}
		}
		
		return propIds;
	}
	
	public void navBar(){
		String storeCode = getPara("storeCode");
		if(StringUtils.isNotEmpty(storeCode)){
			Store store = Store.dao.findByCode(storeCode);
			if(store != null){
				setAttr("role", store.getStoreCode().equals(store.getIndustryCode()));
				setAttr("store", store);
			}
		}
		setAttr("storeCode", storeCode);
		renderJsp("store/navbar.jsp");
	}
}
