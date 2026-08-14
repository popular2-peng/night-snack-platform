<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadRequestOptions } from 'element-plus'
import { Plus, Search, Edit, Delete, FolderAdd, CollectionTag, Picture } from '@element-plus/icons-vue'
import {
  listProductsApi, getProductApi, createProductApi, updateProductApi,
  updateProductStatusApi, deleteProductApi, addSpecApi, removeSpecApi,
  type MerchantProductVO, type ProductFormDTO,
} from '@/apis/product'
import {
  listCategoriesApi, createCategoryApi, updateCategoryApi, deleteCategoryApi,
  type MerchantCategory,
} from '@/apis/category'
import { uploadSignApi, uploadFileToOss } from '@/apis/upload'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
/** 商家 ID = JWT sub（登录时已存 userId） */
const merchantId = Number(authStore.userId) || 0

// ---- 分类 ----
const categories = ref<MerchantCategory[]>([])
const activeCategoryId = ref<number | null>(null) // null = 全部分类
const categoryLoading = ref(false)

const fetchCategories = async () => {
  try {
    const res = await listCategoriesApi()
    categories.value = res.data || []
  } catch { /* 拦截器已提示 */ }
}

// ---- 商品列表 ----
const products = ref<MerchantProductVO[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const loading = ref(false)

const fetchProducts = async () => {
  loading.value = true
  try {
    const res = await listProductsApi({
      merchantId, page: page.value, size: size.value,
      categoryId: activeCategoryId.value ?? undefined,
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
    })
    products.value = res.data.list || []
    total.value = res.data.total || 0
  } catch { ElMessage.error('获取商品列表失败') }
  finally { loading.value = false }
}

const switchCategory = (id: number | null) => {
  activeCategoryId.value = id
  page.value = 1
  fetchProducts()
}
const handleSearch = () => { page.value = 1; fetchProducts() }
const handleReset = () => { keyword.value = ''; statusFilter.value = ''; page.value = 1; fetchProducts() }

// ---- 分类增删改 ----
const categoryDialogVisible = ref(false)
const categoryForm = reactive({ id: null as number | null, name: '', sort: 0 })
const categorySaving = ref(false)

const openCategoryDialog = (cat?: MerchantCategory) => {
  categoryForm.id = cat?.id ?? null
  categoryForm.name = cat?.name ?? ''
  categoryForm.sort = cat?.sort ?? 0
  categoryDialogVisible.value = true
}

const submitCategory = async () => {
  if (!categoryForm.name.trim()) { ElMessage.warning('请输入分类名称'); return }
  categorySaving.value = true
  try {
    if (categoryForm.id) {
      await updateCategoryApi(categoryForm.id, categoryForm.name.trim(), categoryForm.sort)
      ElMessage.success('分类已更新')
    } else {
      await createCategoryApi(categoryForm.name.trim(), categoryForm.sort)
      ElMessage.success('分类已新增')
    }
    categoryDialogVisible.value = false
    fetchCategories()
  } catch { /* 拦截器已提示 */ }
  finally { categorySaving.value = false }
}

const handleDeleteCategory = async (cat: MerchantCategory) => {
  await ElMessageBox.confirm(`确定删除分类「${cat.name}」？分类下的商品不会被删除。`, '删除分类',
    { type: 'warning', confirmButtonText: '确认删除' })
  try {
    await deleteCategoryApi(cat.id)
    ElMessage.success('分类已删除')
    if (activeCategoryId.value === cat.id) switchCategory(null)
    fetchCategories()
  } catch { /* 拦截器已提示 */ }
}

// ---- 商品对话框 ----
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const productSaving = ref(false)
const uploadingImg = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  categoryId: null as number | null,
  price: 0,
  imageUrl: '',
  description: '',
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入菜品名称', trigger: 'blur' },
    { max: 50, message: '名称最多 50 字', trigger: 'blur' },
  ],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
}

/** 规格（编辑态同步远端已保存规格，新增态仅本地行） */
const specRows = ref<{ id?: number; name: string; priceDelta: number; stock: number }[]>([])

const openCreateDialog = () => {
  dialogMode.value = 'create'
  editingId.value = null
  form.name = ''
  form.categoryId = activeCategoryId.value ?? categories.value[0]?.id ?? null
  form.price = 0
  form.imageUrl = ''
  form.description = ''
  specRows.value = []
  dialogVisible.value = true
}

const openEditDialog = async (row: MerchantProductVO) => {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.name = row.name
  form.categoryId = row.categoryId
  form.price = Number(row.price)
  form.imageUrl = row.imageUrl || ''
  form.description = row.description || ''
  try {
    const res = await getProductApi(row.id)
    specRows.value = (res.data.specs || []).map((s) => ({
      id: s.id, name: s.name, priceDelta: Number(s.priceDelta), stock: s.stock,
    }))
  } catch { specRows.value = [] }
  dialogVisible.value = true
}

/** 图片上传（OSS 签名直传，PRODUCT_IMAGE） */
const handleImgUpload = async (options: UploadRequestOptions) => {
  const file = options.file as File
  uploadingImg.value = true
  try {
    const signRes = await uploadSignApi('PRODUCT_IMAGE', file.name)
    const url = await uploadFileToOss(signRes.data, file)
    form.imageUrl = url
    ElMessage.success('图片上传成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '图片上传失败')
  } finally { uploadingImg.value = false }
}

/** 规格行操作 */
const addSpecRow = () => {
  specRows.value.push({ name: '', priceDelta: 0, stock: 0 })
}
const removeSpecRow = (index: number) => {
  specRows.value.splice(index, 1)
}
/** 编辑态：删除已保存规格（立即生效） */
const removeSavedSpec = async (index: number) => {
  const spec = specRows.value[index]
  if (spec.id) {
    try {
      await removeSpecApi(spec.id)
    } catch { return }
  }
  removeSpecRow(index)
}
/** 编辑态：保存当前未持久化的规格行（立即生效） */
const saveNewSpecRows = async () => {
  for (const spec of [...specRows.value]) {
    if (spec.id) continue
    if (!spec.name.trim()) continue
    await addSpecApi(editingId.value!, {
      name: spec.name.trim(), priceDelta: spec.priceDelta || 0, stock: spec.stock || 0,
    })
  }
}

/** 提交商品表单 */
const submitProduct = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const payload: ProductFormDTO = {
    name: form.name.trim(),
    categoryId: form.categoryId!,
    price: form.price,
    imageUrl: form.imageUrl || undefined,
    description: form.description || undefined,
  }
  productSaving.value = true
  try {
    if (dialogMode.value === 'create') {
      const res = await createProductApi(payload)
      // 新增成功后逐条添加规格
      for (const spec of specRows.value) {
        if (!spec.name.trim()) continue
        await addSpecApi(res.data.id, {
          name: spec.name.trim(), priceDelta: spec.priceDelta || 0, stock: spec.stock || 0,
        })
      }
      ElMessage.success('菜品已新增')
    } else {
      await updateProductApi(editingId.value!, payload)
      await saveNewSpecRows()
      ElMessage.success('菜品已更新')
    }
    dialogVisible.value = false
    fetchProducts()
  } catch { /* 拦截器已提示 */ }
  finally { productSaving.value = false }
}

// ---- 状态切换 / 删除 ----
const beforeStatusChange = async (row: MerchantProductVO): Promise<boolean> => {
  const target = row.status === 'ON' ? 'OFF' : 'ON'
  try {
    await ElMessageBox.confirm(
      target === 'OFF' ? `确定停售「${row.name}」？用户端将不可见。` : `确定恢复上架「${row.name}」？`,
      target === 'OFF' ? '停售' : '上架', { type: 'warning', confirmButtonText: '确认' })
  } catch { return false }
  try {
    await updateProductStatusApi(row.id, target)
    ElMessage.success(target === 'OFF' ? '已停售' : '已上架')
    fetchProducts()
    return true
  } catch { return false }
}

const handleDeleteProduct = async (row: MerchantProductVO) => {
  await ElMessageBox.confirm(`确定删除「${row.name}」？其规格将一并删除，此操作不可恢复。`, '删除菜品',
    { type: 'warning', confirmButtonText: '确认删除' })
  try {
    await deleteProductApi(row.id)
    ElMessage.success('已删除')
    fetchProducts()
  } catch { /* 拦截器已提示（如在售会提示先停售） */ }
}

const formatMoney = (n: number) => '¥' + Number(n).toFixed(2)

/** 多余规格名称（表格 tooltip 展示用） */
const extraSpecNames = (specs: { name: string }[]) => specs.map((s) => s.name).join('、')

onMounted(() => {
  fetchCategories()
  fetchProducts()
})
</script>

<template>
  <div class="products-page">
    <!-- ====== 顶栏 ====== -->
    <div class="page-card top-bar fade-in">
      <div class="top-title">
        <h2><el-icon><CollectionTag /></el-icon>菜品管理</h2>
        <span class="top-hint">共 {{ total }} 个菜品</span>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增菜品</el-button>
    </div>

    <!-- ====== 主体：左侧分类 + 右侧菜品表格（苍穹外卖布局） ====== -->
    <div class="main-grid">
      <!-- 左侧分类卡片 -->
      <div class="page-card category-card fade-in">
        <div class="card-head">
          <span class="card-title">菜品分类</span>
          <el-button size="small" type="primary" plain :icon="FolderAdd"
            @click="openCategoryDialog()">新增分类</el-button>
        </div>
        <div class="category-list" v-loading="categoryLoading">
          <div class="category-item" :class="{ active: activeCategoryId === null }"
            @click="switchCategory(null)">
            <span class="cat-name">全部菜品</span>
          </div>
          <div v-for="cat in categories" :key="cat.id" class="category-item"
            :class="{ active: activeCategoryId === cat.id }" @click="switchCategory(cat.id)">
            <span class="cat-name">{{ cat.name }}</span>
            <span class="cat-actions">
              <el-icon class="cat-btn" @click.stop="openCategoryDialog(cat)"><Edit /></el-icon>
              <el-icon class="cat-btn danger" @click.stop="handleDeleteCategory(cat)"><Delete /></el-icon>
            </span>
          </div>
          <el-empty v-if="!categories.length && !categoryLoading" description="暂无分类" :image-size="60" />
        </div>
      </div>

      <!-- 右侧菜品表格 -->
      <div class="page-card product-card fade-in">
        <div class="toolbar">
          <el-input v-model="keyword" placeholder="搜索菜品名称" clearable style="width: 220px"
            :prefix-icon="Search" @keyup.enter="handleSearch" @clear="handleSearch" />
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 130px" @change="handleSearch">
            <el-option label="在售" value="ON" />
            <el-option label="停售" value="OFF" />
          </el-select>
          <el-button type="primary" plain @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>

        <el-table :data="products" stripe v-loading="loading" style="width: 100%">
          <el-table-column label="图片" width="80" align="center">
            <template #default="{ row }">
              <el-image v-if="row.imageUrl" :src="row.imageUrl" :preview-src-list="[row.imageUrl]"
                fit="cover" style="width:46px;height:46px;border-radius:8px" :preview-teleported="true" />
              <div v-else class="img-placeholder">🍜</div>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="菜品名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="categoryName" label="分类" width="110" align="center">
            <template #default="{ row }">
              <el-tag size="small" effect="plain" type="info">{{ row.categoryName || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="价格" width="100" align="center">
            <template #default="{ row }">
              <span class="price-text">{{ formatMoney(row.price) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="规格" min-width="170">
            <template #default="{ row }">
              <template v-if="row.specs?.length">
                <el-tag v-for="s in row.specs.slice(0, 2)" :key="s.id" size="small" class="spec-tag">
                  {{ s.name }} {{ Number(s.priceDelta) > 0 ? '+' + formatMoney(s.priceDelta) : '' }} / 库存{{ s.stock }}
                </el-tag>
                <el-tooltip v-if="row.specs.length > 2" :content="extraSpecNames(row.specs.slice(2))">
                  <el-tag size="small" type="info">+{{ row.specs.length - 2 }}</el-tag>
                </el-tooltip>
              </template>
              <span v-else class="no-spec">无规格</span>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ row.description || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-switch :model-value="row.status === 'ON'" :before-change="() => beforeStatusChange(row)"
                style="--el-switch-on-color: #52C41A" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" plain @click="openEditDialog(row)">修改</el-button>
              <el-button size="small" type="danger" plain @click="handleDeleteProduct(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap" v-if="total > size">
          <el-pagination
            v-model:current-page="page" :page-size="size" :total="total"
            layout="total, prev, pager, next" background @current-change="fetchProducts"
          />
        </div>
      </div>
    </div>

    <!-- ====== 分类对话框 ====== -->
    <el-dialog v-model="categoryDialogVisible" :title="categoryForm.id ? '修改分类' : '新增分类'" width="420px">
      <el-form label-width="80px">
        <el-form-item label="分类名称">
          <el-input v-model="categoryForm.name" maxlength="20" placeholder="如：招牌套餐" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sort" :min="0" :max="999" controls-position="right" style="width: 160px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="categorySaving" @click="submitCategory">确定</el-button>
      </template>
    </el-dialog>

    <!-- ====== 菜品对话框（新增/修改） ====== -->
    <el-dialog v-model="dialogVisible" class="product-dialog"
      width="760px" :close-on-click-modal="false" align-center>
      <template #header>
        <div class="dlg-header">
          <span class="dlg-icon"><el-icon :size="20"><CollectionTag /></el-icon></span>
          <div class="dlg-title-wrap">
            <span class="dlg-title">{{ dialogMode === 'create' ? '新增菜品' : '修改菜品' }}</span>
            <span class="dlg-sub">
              {{ dialogMode === 'create' ? '创建后默认上架，可继续添加规格' : '修改基础信息，规格增删即时生效' }}
            </span>
          </div>
          <el-tag size="small" :type="dialogMode === 'create' ? 'primary' : 'warning'" effect="light" round>
            {{ dialogMode === 'create' ? '新建' : '编辑' }}
          </el-tag>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="dlg-form">
        <!-- 图片 + 基本信息 -->
        <div class="dlg-top">
          <div class="img-zone">
            <span class="zone-label">菜品图片</span>
            <el-upload accept=".jpg,.jpeg,.png" :show-file-list="false" :http-request="handleImgUpload">
              <div class="img-uploader" v-loading="uploadingImg">
                <img v-if="form.imageUrl" :src="form.imageUrl" class="img-preview" alt="菜品图片" />
                <div v-else class="img-empty">
                  <el-icon :size="26"><Picture /></el-icon>
                  <span>上传图片</span>
                </div>
                <div v-if="form.imageUrl" class="img-mask">
                  <el-icon><Picture /></el-icon> 更换
                </div>
              </div>
            </el-upload>
            <p class="zone-tip">支持 JPG / PNG，建议 1:1 正方形</p>
          </div>

          <div class="info-zone">
            <el-form-item label="菜品名称" prop="name">
              <el-input v-model="form.name" maxlength="50" placeholder="请输入菜品名称" size="large" />
            </el-form-item>
            <el-form-item label="菜品分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%" size="large">
                <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="价格（元）" prop="price">
              <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1"
                controls-position="right" style="width: 100%" size="large" />
            </el-form-item>
          </div>
        </div>

        <el-form-item label="菜品描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="200"
            show-word-limit placeholder="口味、食材、推荐吃法等（可选，将展示在用户端）" />
        </el-form-item>

        <!-- 规格设置 -->
        <div class="spec-panel">
          <div class="spec-head">
            <span class="zone-label">规格设置</span>
            <span class="spec-hint">{{ dialogMode === 'edit' ? '增删即时生效' : '保存菜品后创建' }}</span>
            <el-button size="small" type="primary" plain :icon="Plus" @click="addSpecRow">添加规格</el-button>
          </div>

          <div v-if="specRows.length" class="spec-rows">
            <div v-for="(spec, i) in specRows" :key="i" class="spec-row">
              <span class="spec-index">{{ i + 1 }}</span>
              <el-input v-model="spec.name" placeholder="规格名（如：大份）" style="width: 160px" />
              <el-input-number v-model="spec.priceDelta" :min="0" :precision="2" :step="0.5"
                controls-position="right" style="width: 130px" />
              <span class="spec-unit">加价(元)</span>
              <el-input-number v-model="spec.stock" :min="0" :step="1" controls-position="right"
                style="width: 110px" />
              <span class="spec-unit">库存</span>
              <el-button size="small" type="danger" plain :icon="Delete" circle
                @click="dialogMode === 'edit' ? removeSavedSpec(i) : removeSpecRow(i)" />
            </div>
          </div>
          <div v-else class="spec-empty" @click="addSpecRow">
            <el-icon><Plus /></el-icon>
            <span>还没有规格，点击添加（加价 0 表示按基础价出售）</span>
          </div>
        </div>
      </el-form>

      <template #footer>
        <div class="dlg-footer">
          <el-button size="large" @click="dialogVisible = false">取消</el-button>
          <el-button size="large" type="primary" class="dlg-save" :loading="productSaving" @click="submitProduct">
            {{ dialogMode === 'create' ? '保存并上架' : '保存修改' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.products-page { max-width: 1280px; margin: 0 auto; }

/* ---- 顶栏 ---- */
.top-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; margin-bottom: 14px;
}
.top-title { display: flex; align-items: baseline; gap: 12px; }
.top-title h2 {
  margin: 0; font-size: 18px; font-weight: 600;
  display: flex; align-items: center; gap: 8px;
}
.top-title h2 .el-icon { color: var(--primary); }
.top-hint { font-size: 12px; color: var(--text-secondary); }

/* ---- 主布局 ---- */
.main-grid {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 14px;
  align-items: start;
}

/* ---- 左侧分类卡片 ---- */
.category-card { padding: 16px; }
.card-head {
  display: flex; align-items: center; justify-content: space-between;
  padding-bottom: 12px; margin-bottom: 10px;
  border-bottom: 1px solid var(--border-light);
}
.card-title { font-size: 15px; font-weight: 600; color: var(--text-primary); }
.category-list { max-height: 560px; overflow-y: auto; }
.category-item {
  display: flex; align-items: center; justify-content: space-between;
  height: 40px; padding: 0 10px 0 14px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-regular);
  transition: all 0.15s ease;
  position: relative;
}
.category-item:hover { background: var(--bg); }
.category-item.active {
  background: var(--primary-bg);
  color: var(--primary-dark);
  font-weight: 600;
}
.category-item.active::before {
  content: '';
  position: absolute;
  left: 0; top: 10px; bottom: 10px;
  width: 3px;
  background: var(--primary);
  border-radius: 0 3px 3px 0;
}
.cat-name {
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  flex: 1;
}
.cat-actions { display: none; gap: 8px; }
.category-item:hover .cat-actions { display: flex; }
.cat-btn { font-size: 14px; color: var(--text-secondary); }
.cat-btn:hover { color: var(--primary); }
.cat-btn.danger:hover { color: var(--danger); }

/* ---- 右侧商品卡片 ---- */
.product-card { padding: 16px 20px; min-height: 480px; }
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; }
.price-text { color: var(--primary-dark); font-weight: 600; font-variant-numeric: tabular-nums; }
.img-placeholder {
  width: 46px; height: 46px; border-radius: 8px;
  background: var(--bg);
  display: flex; align-items: center; justify-content: center;
  font-size: 20px;
}
.spec-tag { margin-right: 6px; }
.no-spec { color: var(--text-placeholder); font-size: 12px; }
.pagination-wrap { display: flex; justify-content: center; padding: 14px 0 4px; }

/* ---- 菜品对话框 ---- */
.product-dialog {
  border-radius: 16px;
  overflow: hidden;
}
.product-dialog :deep(.el-dialog__header) { padding: 0; margin: 0; }
.product-dialog :deep(.el-dialog__body) { padding: 20px 28px 8px; }

.dlg-header {
  position: relative;
  display: flex; align-items: center; gap: 12px;
  padding: 18px 24px;
  background: linear-gradient(135deg, #FFF9F0 0%, #FFF3DF 60%, #FFE9C7 100%);
  border-bottom: 1px solid var(--border-light);
}
.dlg-icon {
  width: 40px; height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #F59A23, #E07F0E);
  color: #fff;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 12px rgba(245, 154, 35, 0.35);
  flex-shrink: 0;
}
.dlg-title-wrap { display: flex; flex-direction: column; gap: 2px; flex: 1; min-width: 0; }
.dlg-title { font-size: 17px; font-weight: 700; color: var(--text-primary); }
.dlg-sub { font-size: 12px; color: var(--text-secondary); }
/* 使用默认关闭按钮（Element Plus 自带），仅调整位置与颜色适配渐变头部 */
.product-dialog :deep(.el-dialog__headerbtn) {
  top: 22px;
}
.product-dialog :deep(.el-dialog__headerbtn .el-icon) {
  color: var(--text-regular);
}

.dlg-top {
  display: grid;
  grid-template-columns: 160px 1fr;
  gap: 28px;
  margin-bottom: 6px;
}
.zone-label {
  display: block;
  font-size: 13px; font-weight: 600; color: var(--text-regular);
  margin-bottom: 8px;
}
.zone-tip { margin: 8px 0 0; font-size: 12px; color: var(--text-secondary); line-height: 1.5; }

/* 图片上传 */
.img-zone { display: flex; flex-direction: column; align-items: center; }
.img-uploader {
  width: 150px; height: 150px;
  border: 2px dashed var(--border-color);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s ease;
  background: var(--bg);
}
.img-uploader:hover { border-color: var(--primary); box-shadow: 0 4px 14px rgba(245, 154, 35, 0.15); }
.img-preview { width: 100%; height: 100%; object-fit: cover; }
.img-empty {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  font-size: 13px; color: var(--text-secondary);
}
.img-empty .el-icon { color: var(--text-placeholder); }
.img-mask {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center; gap: 5px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff; font-size: 13px;
  opacity: 0; transition: opacity 0.2s ease;
}
.img-uploader:hover .img-mask { opacity: 1; }

/* 规格面板 */
.spec-panel {
  margin-top: 8px;
  background: var(--bg);
  border-radius: 14px;
  padding: 16px 18px;
}
.spec-head {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: 12px;
}
.spec-head .zone-label { margin-bottom: 0; }
.spec-hint { font-size: 12px; color: var(--text-secondary); flex: 1; }
.spec-rows { display: flex; flex-direction: column; gap: 10px; }
.spec-row {
  display: flex; align-items: center; gap: 10px;
  background: var(--bg-white);
  border-radius: 10px;
  padding: 10px 12px;
  box-shadow: var(--shadow-xs);
}
.spec-index {
  width: 22px; height: 22px;
  border-radius: 50%;
  background: var(--primary-bg);
  color: var(--primary-dark);
  font-size: 12px; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.spec-unit { font-size: 12px; color: var(--text-secondary); flex-shrink: 0; }
.spec-empty {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  height: 64px;
  border: 1.5px dashed var(--border-color);
  border-radius: 10px;
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 13px;
  transition: all 0.2s ease;
}
.spec-empty:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-bg);
}

/* 底部 */
.dlg-footer { display: flex; justify-content: flex-end; gap: 12px; }
.dlg-save {
  min-width: 140px;
  background: linear-gradient(135deg, #F59A23, #E07F0E);
  border: none;
}
.dlg-save:hover { opacity: 0.9; }

@media (max-width: 700px) {
  .dlg-top { grid-template-columns: 1fr; }
  .spec-row { flex-wrap: wrap; }
}
</style>
