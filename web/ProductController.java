package com.ichunshen.dolook.module.product.web;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.base.CharMatcher;
import com.ichunshen.dolook.module.industry.store.model.Store;
import com.ichunshen.dolook.module.invest.model.MercantileBill;
import com.ichunshen.dolook.module.marketing.crowdfunding.model.Crowdfund;
import com.ichunshen.dolook.module.product.comment.model.Comment;
import com.ichunshen.dolook.module.product.model.Postage;
import com.ichunshen.dolook.module.product.model.Product;
import com.ichunshen.dolook.module.product.model.ProductImage;
import com.ichunshen.dolook.module.product.model.ProductNum;
import com.ichunshen.dolook.module.product.model.ProductProperty;
import com.ichunshen.dolook.module.product.model.ProductPropertyValue;
import com.ichunshen.dolook.module.product.model.ProductSku;
import com.ichunshen.dolook.module.product.model.ProductUserView;
import com.ichunshen.dolook.support.SimpleErrorType;
import com.ichunshen.dolook.temp.DoLookCache;
import com.ichunshen.dolook.temp.bean.UserBean;
import com.ichunshen.dolook.utils.CommonUtil;
import com.jfinal.core.Controller;
import com.jfinal.kit.JsonKit;
import com.jfinal.plugin.activerecord.Page;

import cn.joy.framework.kits.RuleKit;
import cn.joy.framework.kits.StringKit;
import cn.joy.plugin.jfinal.annotation.Ctrl;
import cn.joy.framework.rule.RuleParam;
import cn.joy.framework.rule.RuleResult;
import cn.joy.framework.server.RouteManager;

@Ctrl(url = "/product")
public class ProductController extends Controller {
	Logger logger = Logger.getLogger(ProductController.class);

	/**
	 * 商品列表接口 请求参数：storeCode 店铺，page 默认为第一页，groupId 默认全部分类，number 默认每页10
	 */
	public void list() {
		RuleResult result = RuleResult.create();

		// 参数检查
		String storeCode = getPara("storeCode", "").trim();
		String page = getPara("page", "").trim();
		String number = getPara("number", "").trim();
		String groupId = getPara("groupId", "").trim();
		String categoryId = getPara("categoryId", "").trim();
		String categoryLevel = getPara("categoryLevel", "").trim();

		if (StringKit.isEmpty(storeCode)) {
			renderText(result.fail(SimpleErrorType.MISS_PARAMETER).toJSON());
			return;
		}

		if (!StringKit.isEmpty(page)) {
			page = CharMatcher.JAVA_DIGIT.retainFrom(page);
		}
		page = StringKit.isEmpty(page) ? "1" : page;

		if (!StringKit.isEmpty(number)) {
			number = CharMatcher.JAVA_DIGIT.retainFrom(number);
		}
		page = StringKit.isEmpty(page) ? "10" : page;

		// 查询
		Page<Product> products = Product.dao.paginateByStoreAndGroupAndCategory(Integer.parseInt(page), Integer.parseInt(number), storeCode, groupId, categoryId, categoryLevel);
		for (Product pd : products.getList()) {
			CommonUtil.productImgTo(pd);
		}
		renderText(result.success().putContent(products).toJSON());
	}

	/**
	 * 商品详情页面 请求参数：storeCode 店铺号，pcode 商品号
	 */
	public void details() {
		// 参数检查
		String storeCode = getPara("storeCode");
		String pcode = getPara("pcode");

		if (StringKit.isEmpty(pcode) || StringKit.isEmpty(storeCode)) {
			setAttr("errCode", SimpleErrorType.MISS_PARAMETER);
			setAttr("errMsg", "missing parameter");
			renderJsp("/error.jsp");
			return;
		}

		MercantileBill bill = MercantileBill.dao.getTradeBillByPcode(pcode);
		if (bill != null) {
			redirect("/trade/detail?spcode=" + bill.getSpCode());
			return;
		}

		// 商品,店铺是否存在
		Product product = Product.dao.getByCode(pcode);
		Store visitStore = Store.dao.findByCode(storeCode);// 当前访问店铺
		if (product == null || visitStore == null) {
			setAttr("errCode", "");
			setAttr("errMsg", "product not exist or store not exist!");
			renderJsp("/error.jsp");
			return;
		}

		// 商品所属店铺
		Store productStore = Store.dao.findByCode(product.getStr("store_code"));
		// 轮播图
		List<ProductImage> images = ProductImage.dao.findByProduct(product);
		// 邮费
		Postage postage = Postage.dao.findById(product.getInt("postage_id"));
		// 属性
		List<ProductProperty> properties = ProductProperty.dao.findByProduct(pcode);
		for (ProductProperty pp : properties) {
			List<ProductPropertyValue> propertyValues = ProductPropertyValue.dao.findByProductAndProterty(pcode, pp.getLong("id").toString());
			pp.put("values", propertyValues);
		}
		// SKU
		List<ProductSku> skus = ProductSku.dao.findByProduct(pcode);
		// 库存状态
		int quantity = 0;
		if (skus.isEmpty()) {
			quantity = product.getInt("quantity");
		} else {
			for (ProductSku sku : skus) {
				int num = sku.getInt("quantity");
				quantity += num;
			}
		}
		setAttr("quantity", quantity);

		// 沟通URL
		String companyUrl = RouteManager.getServerURLByCompanyCode(product.getStr("store_code"));
		String communicateUrl = companyUrl + "/thing.do";

		String imagesList = product.getStr("image");
		String[] listImg = null;
		if (imagesList != null) {
			listImg = imagesList.split(",");
		}
		String img = "";
		if (listImg != null) {
			img = listImg[0];
		}

		// 如果是众筹，查询众筹信息
		if ("1".equals(product.getStr("goods_value"))) {
			Crowdfund crowdfund = Crowdfund.dao.getByProduct(pcode);
			Map<String, Object> crowdfundMap = Crowdfund.toViewModel(crowdfund, product, productStore);
			setAttr("crowdfundMap", crowdfundMap);
		}

		ProductNum pn = ProductNum.dao.getProductNumByPcode(pcode);
		if (pn == null) {
			pn = new ProductNum();
			pn.set("pcode", pcode);
			pn.set("pv", 1);
			pn.set("uv", 1);
			pn.set("comment_num", 0);
			pn.set("collect", 0);
			pn.set("sucess_order_num", 0);
			pn.set("order_num", 0);
			ProductNum.dao.addProductNum(pn);
		} else {
			pn.set("pv", pn.getInt("pv") + 1);
			pn.set("uv", pn.getInt("uv") + 1);
			ProductNum.dao.updateProductNum(pn);
		}

		// 评论
		if (pn.getInt("comment_num") > 0) {
			Page<Comment> comments = Comment.dao.paginateByProduct(1, 1, pcode);
			if (comments.getTotalRow() != 0) {
				setAttr("comment", comments.getList().get(0));
			}
		}

		// 测试评论图片
		// Comment comment = Comment.dao.findById(82);
		// logger.info("images ====== " + CommonUtil.getImageUrl("79995718",
		// comment.getStr("images")));

		setAttr("productNum", pn);
		setAttr("goodRate", Comment.dao.getGoodRate(pcode));

		setAttr("img", img);
		setAttr("listImg", listImg);
		setAttr("product", product);
		setAttr("visitStore", visitStore);
		setAttr("images", images);
		setAttr("postage", postage);
		setAttr("properties", properties);
		setAttr("skus", JsonKit.toJson(skus));
		setAttr("productStore", productStore);
		setAttr("communicateUrl", communicateUrl);
		addProductUserView(product);

		renderJsp("details.jsp");
	}

	private void addProductUserView(Product product) {
		UserBean userBean = DoLookCache.getUserFromSession(this.getRequest());
		if (userBean == null) {
			return;
		}
		ProductUserView productView = ProductUserView.dao.getProductViewByLoginId(userBean.getLoginId(), product.getPcode());
		Date now = new Date();
		if (productView == null) {
			productView = new ProductUserView();
			productView.setLoginId(userBean.getLoginId());
			productView.setCreateTime(now);
			productView.setUpdateTime(now);
			productView.setPv(1);
			productView.setPcode(product.getPcode());
			productView.setPname(product.getPname());
			productView.setStoreCode(product.getStoreCode());
			productView.setIndustryCode(product.getIndustryCode());
			productView.save();
		} else {
			productView.setPname(product.getPname());
			productView.setStoreCode(product.getStoreCode());
			productView.setIndustryCode(product.getIndustryCode());

			productView.setUpdateTime(now);
			productView.setPv(productView.getPv() + 1);
			productView.update();
		}
	}

	/**
	 * 获得商品详情 请求参数：pcode 商品号
	 */
	public void getDescOfProduct() {
		String pcode = getPara("pcode");
		if (StringKit.isEmpty(pcode)) {
			renderJsp("/error.jsp");
			return;
		}

		Product product = Product.dao.getByCode(pcode);
		if (product == null) {
			renderJsp("/error.jsp");
			return;
		}

		renderText(product.getStr("pdesc") == null ? "" : product.getStr("pdesc"));
	}

	public void confirmProtocol() {
		renderJsp("confirmProtocol.jsp");
	}

	public void hotFix() {
		RuleParam pars = RuleParam.create();
		pars.put("key", "");
		pars.put("start", "0");
		pars.put("count", "10");
		pars.put("storecode", "13926");
		pars.put("cateName", "部件");
		// RuleKit.invokeRule(null, "NONE",
		// "product.productService#findRepairProduct", pars);
		//uzw9ifpici
		pars.put("cate", getPara("cate"));//86314
		RuleResult ruslut = RuleKit.invokeRule(null, "uvhzog649m", "product.productService#lookProduct", pars);
		//System.out.println(ruslut.toJSON());
		renderText(ruslut.toJSON());
	}
}
