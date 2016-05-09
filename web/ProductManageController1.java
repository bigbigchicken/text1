package com.ichunshen.dolook.module.product.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import com.ichunshen.dolook.module.industry.store.model.Menu;
import com.ichunshen.dolook.module.industry.store.model.Store;
import com.ichunshen.dolook.module.product.model.Product;
import com.ichunshen.dolook.utils.CommonUtil;
import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.tx.Tx;

import cn.joy.framework.rule.RuleResult;
import cn.joy.plugin.jfinal.annotation.Ctrl;

@Ctrl(url = "/productManage1")
public class ProductManageController1 extends Controller {
	public void index() {
		// String storeCode = getPara("storeCode");
		// if (StringUtils.isEmpty(storeCode)) {
		// renderJsp("");
		// return;
		// }
		// Store store = Store.dao.findByCode(storeCode);
		// if (store == null) {
		// renderJsp("");
		// return;
		// }
		// String industryCode = store.getIndustryCode();
		// setAttr("storeCode", storeCode);
		// setAttr("industyCode", industryCode);
		// renderJsp("product_manage/product_manage.jsp");
		// String industryCode="70197";
		// long
		// num=Product.dao.countUnverifiedProductsByIndustryCode(industryCode);
		// System.out.println(num);
		// renderText("dddd");
		// http://211.95.45.110:48889/fileManager/uploadImage//0/00/00/0000001520151228065623779645.png
		// http://localhost:8080/dolook/hcpc/images/sales-champion.png
	}

	/**
	 * 根据行业号，查询该行业所有未删除，且已通过审核商品
	 */
	public void industryProductsList() {
		String industryCode = getPara("industryCode");
		List<Product> i_products = Product.dao.getVeriPassedProductsByIndustryCode(industryCode);
		for (Product product : i_products) {
			CommonUtil.productImgTo(product);
		}
		setAttr("i_products", i_products);
		renderJsp("product_manage/product_list_industry.jsp");
	}

	/**
	 * 根据行业号，查询该行业未审核或审核不通过商品
	 */
	public void industryProductsVerification() {
		String industryCode = getPara("industryCode");
		List<Product> v_products = Product.dao.getUnveriAndVeriDeniedProductsByIndustryCode(industryCode);
		for (Product product : v_products) {
			CommonUtil.productImgTo(product);
		}
		setAttr("v_products", v_products);
		renderJsp("product_manage/product_verify.jsp");

		// 分页
		int pageSize = 10;
		long num = Product.dao.countUnverifiedProductsByIndustryCode(industryCode);
		int pageNum = getPageNum(num, pageSize);
		// int nextPage = 1;

		int page = getParaToInt("p", 1);
		if (page < 0) {
			page = 1;
		}
		// nextPage = page + 1;
		if (page > pageNum) {
			page = pageNum;
		}
		// if (nextPage > pageNum) {
		// nextPage = pageNum;
		// }
		// int sn = (page - 1) < 0 ? 0 : (page - 1) * pageSize;

		setAttr("industryCode", industryCode);
		setAttr("pageNum", pageNum);
		setAttr("page", page);

	}

	/**
	 * 产品上架（可批量）
	 */
	@Before(Tx.class)
	public void batchPutAway() {
		String[] ids = getParaValues("id");
		for (String id : ids) {
			boolean result = Product.dao.getById(id).set("p_status", "1").set("modify_time", new Date()).update();
			renderJson(result);
		}
	}

	/**
	 * 产品下架（可批量）
	 */
	@Before(Tx.class)
	public void batchSoldOut() {
		String[] ids = getParaValues("id");
		for (String id : ids) {
			boolean result = Product.dao.getById(id).set("p_status", "2").set("modify_time", new Date()).update();
			renderJson(result);
		}
	}

	/**
	 * 产品删除（可以批量）
	 */
	@Before(Tx.class)
	public void batchDelProduct() {
		String[] ids = getParaValues("id");
		for (String id : ids) {
			boolean result = Product.dao.getById(id).set("del_flag", "1").set("modify_time", new Date()).update();
			renderJson(result);
		}
		// renderJsp("product_manage/product_list_industry.jsp");
	}

	/**
	 * 审核通过（可以批量）
	 */
	@Before(Tx.class)
	public void batchVeriPassed() {
		String[] ids = getParaValues("id");
		for (String id : ids) {
			boolean result = Product.dao.getById(id).set("p_status", "0").set("modify_time", new Date()).update();
			renderJson(result);
		}
	}

	/**
	 * 审核不通过（可以批量）
	 */
	@Before(Tx.class)
	public void batchVeriDenied() {
		String[] ids = getParaValues("id");
		for (String id : ids) {
			boolean result = Product.dao.getById(id).set("p_status", "5").set("modify_time", new Date()).update();
			renderJson(result);
		}
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
}
