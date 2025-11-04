import { createContext, useContext, useMemo, useState, useEffect } from "react";
import type { ReactNode } from "react";
import { TOKEN_SYMBOL } from "@/lib/currency";

export type Lang = "en" | "ja";

type Dict = Record<string, Record<Lang, string>>;

const dict: Dict = {
  "nav.overview": { en: "Overview", ja: "概要" },
  "nav.dashboard": { en: "Dashboard", ja: "ダッシュボード" },
  "nav.banking": { en: "Banking", ja: "バンキング" },
  "nav.profile": { en: "My Profile", ja: "マイプロフィール" },
  "nav.products": { en: "Products & Pricing", ja: "商品と価格" },
  "nav.calculator": { en: "FD Calculator", ja: "定期預金計算機" },
  "nav.accounts": { en: "My Accounts", ja: "口座" },
  "nav.admin": { en: "Administration", ja: "管理" },
  "nav.admin.dashboard": { en: "Admin Dashboard", ja: "管理ダッシュボード" },
  "nav.allAccounts": { en: "All Accounts", ja: "すべての口座" },
  "nav.openAccount": { en: "Open Account", ja: "口座を開く" },
  "nav.stablecoin": { en: "CashCached", ja: "CashCached" },
  "brand.name": { en: "CashCached", ja: "CashCached" },
  "brand.subtitle": {
    en: "CashCached Digital Banking",
    ja: "CashCached デジタルバンキング",
  },
  "action.logout": { en: "Log out", ja: "ログアウト" },
  "action.refresh": { en: "Refresh", ja: "更新" },
  "action.exportCsv": { en: "Export CSV", ja: "CSV エクスポート" },
  "action.close": { en: "Close", ja: "閉じる" },
  "action.viewDetails": { en: "View Details", ja: "詳細を見る" },
  "accounts.all": { en: "All Accounts", ja: "すべての口座" },
  "accounts.mine": { en: "My Accounts", ja: "マイ口座" },
  "accounts.subtitle.admin": {
    en: "Manage and monitor all customer accounts",
    ja: "すべての顧客口座を管理・監視",
  },
  "accounts.subtitle.customer": {
    en: "View and manage your fixed deposit accounts",
    ja: "自分の定期預金口座を表示・管理",
  },
  "accounts.openNew": { en: "Open New Account", ja: "新規口座開設" },
  "accounts.searchPlaceholder": {
    en: "Search accounts...",
    ja: "口座を検索...",
  },
  "accounts.filter.status": { en: "Status", ja: "ステータス" },
  "accounts.filter.allStatus": { en: "All Status", ja: "すべてのステータス" },
  "accounts.filter.type": { en: "Type", ja: "種類" },
  "accounts.filter.allTypes": { en: "All Types", ja: "すべての種類" },
  "status.active": { en: "Active", ja: "有効" },
  "status.matured": { en: "Matured", ja: "満期" },
  "status.closed": { en: "Closed", ja: "解約済み" },
  "status.inactive": { en: "Inactive", ja: "無効" },
  "type.fd": { en: "Fixed Deposit", ja: "定期預金" },
  "type.rd": { en: "Recurring Deposit", ja: "積立預金" },
  "type.savings": { en: "Savings", ja: "普通預金" },
  "accounts.empty.title": {
    en: "No accounts found",
    ja: "口座が見つかりません",
  },
  "accounts.empty.adjustFilters": {
    en: "Try adjusting your search or filter criteria",
    ja: "検索やフィルター条件を調整してください",
  },
  "accounts.empty.noneAdmin": {
    en: "No accounts are currently registered",
    ja: "現在登録されている口座はありません",
  },
  "accounts.empty.noneCustomer": {
    en: "You don't have any accounts yet. Open your first account to get started.",
    ja: "まだ口座がありません。まずは口座を開設してください。",
  },
  "accounts.card.account": { en: "Account", ja: "口座番号" },
  "accounts.card.currentBalance": { en: "Current Balance", ja: "現在残高" },
  "accounts.card.interestRate": { en: "Interest Rate", ja: "金利" },
  "accounts.card.principal": { en: "Principal Amount", ja: "元本" },
  "accounts.card.maturityDate": { en: "Maturity Date", ja: "満期日" },
  "accounts.card.opened": { en: "Opened", ja: "開設日" },
  "details.title": { en: "Account Details", ja: "口座詳細" },
  "details.back": { en: "Back to Accounts", ja: "口座一覧に戻る" },
  "details.tabs.overview": { en: "Overview", ja: "概要" },
  "details.tabs.transactions": { en: "Transactions", ja: "取引" },
  "details.tabs.documents": { en: "Documents", ja: "書類" },
  "details.info": { en: "Account Information", ja: "口座情報" },
  "details.accountNumber": { en: "Account Number", ja: "口座番号" },
  "details.accountType": { en: "Account Type", ja: "口座種類" },
  "details.productName": { en: "Product Name", ja: "商品名" },
  "details.status": { en: "Status", ja: "ステータス" },
  "details.financial": { en: "Financial Details", ja: "財務情報" },
  "details.currentBalance": { en: "Current Balance", ja: "現在残高" },
  "details.principal": { en: "Principal Amount", ja: "元本" },
  "details.interestRate": { en: "Interest Rate", ja: "金利" },
  "details.interestEarned": { en: "Interest Earned", ja: "獲得利息" },
  "details.txn.title": { en: "Transaction History", ja: "取引履歴" },
  "details.txn.subtitle": {
    en: "View all transactions for this account",
    ja: "この口座の全取引を表示",
  },
  "details.txn.load": { en: "Load Transactions", ja: "取引を読み込む" },
  "details.txn.empty": { en: "No transactions found", ja: "取引はありません" },
  "details.dates": { en: "Key Dates", ja: "重要な日付" },
  "details.opened": { en: "Account Opened", ja: "開設日" },
  "details.maturityDate": { en: "Maturity Date", ja: "満期日" },
  "details.daysToMaturity": { en: "Days to Maturity", ja: "満期までの日数" },
  "details.customerInfo": { en: "Customer Information", ja: "顧客情報" },
  "details.deposit.label": { en: "Deposit", ja: "入金" },
  "details.deposit.action": { en: "Add", ja: "追加" },
  "details.withdraw.label": { en: "Withdraw", ja: "出金" },
  "details.withdraw.action": { en: "Withdraw", ja: "出金する" },
  "details.notFound.title": {
    en: "Account not found",
    ja: "口座が見つかりません",
  },
  "details.notFound.desc": {
    en: "The account you're looking for doesn't exist or you don't have permission to view it.",
    ja: "お探しの口座は存在しないか、表示権限がありません。",
  },
  "action.download": { en: "Download", ja: "ダウンロード" },
  "action.share": { en: "Share", ja: "共有" },
  "calculator.title": { en: "FD Calculator", ja: "定期預金計算機" },
  "calculator.subtitle": {
    en: "Calculate your fixed deposit returns and plan your investments",
    ja: "定期預金のリターンを計算し、投資計画を立てましょう",
  },
  "calculator.badge": { en: "Investment Calculator", ja: "投資計算機" },
  "calculator.calculateHeadline": {
    en: "Calculate FD Returns",
    ja: "定期預金のリターンを計算",
  },
  "calculator.section.inputs": {
    en: "Simulation Inputs",
    ja: "シミュレーション入力",
  },
  "calculator.section.inputsDesc": {
    en: "Choose a product and fill in the deposit details to run the projection.",
    ja: "商品を選び、預入条件を入力してシミュレーションを実行します。",
  },
  "calculator.product": { en: "Product", ja: "商品" },
  "calculator.principal": {
    en: `Principal Amount (${TOKEN_SYMBOL})`,
    ja: `元本 (${TOKEN_SYMBOL})`,
  },
  "calculator.principalTokens": {
    en: `Principal Amount (${TOKEN_SYMBOL})`,
    ja: `元本 (${TOKEN_SYMBOL})`,
  },
  "calculator.tenureYears": { en: "Tenure (Years)", ja: "期間 (年)" },
  "calculator.compounding": { en: "Compounding", ja: "複利頻度" },
  "calculator.monthly": { en: "Monthly", ja: "毎月" },
  "calculator.quarterly": { en: "Quarterly", ja: "四半期" },
  "calculator.yearly": { en: "Yearly", ja: "毎年" },
  "calculator.calculate": { en: "Calculate", ja: "計算する" },
  "calculator.results": { en: "Results", ja: "結果" },
  "calculator.result.principal": {
    en: `Principal (${TOKEN_SYMBOL})`,
    ja: `元本 (${TOKEN_SYMBOL})`,
  },
  "calculator.result.interest": {
    en: `Interest Earned (${TOKEN_SYMBOL})`,
    ja: `獲得利息 (${TOKEN_SYMBOL})`,
  },
  "calculator.result.maturity": {
    en: `Maturity Amount (${TOKEN_SYMBOL})`,
    ja: `満期金額 (${TOKEN_SYMBOL})`,
  },
  "calculator.calculating": { en: "Calculating...", ja: "計算中..." },
  "calculator.calculateReturns": { en: "Calculate", ja: "計算する" },
  "calculator.quickCalculations": {
    en: "Quick Calculations",
    ja: "クイック計算",
  },
  "calculator.quick.title": { en: "Quick Scenarios", ja: "クイックシナリオ" },
  "calculator.quick.subtitle": {
    en: "Pre-built examples to explore typical deposits",
    ja: "代表的な預入例をすばやく確認できます。",
  },
  "calculator.commonFdScenarios": {
    en: "Common FD scenarios",
    ja: "一般的な定期預金の例",
  },
  "calculator.calculationResults": {
    en: "Calculation Results",
    ja: "計算結果",
  },
  "calculator.investmentBreakdown": {
    en: "Investment breakdown",
    ja: "投資内訳",
  },
  "calculator.result.maturityAmount": { en: "Maturity Amount", ja: "満期金額" },
  "calculator.result.tenure": { en: "Tenure", ja: "期間" },
  "calculator.result.years": { en: "years", ja: "年" },
  "calculator.result.effectiveRate": { en: "Effective Rate", ja: "実効利回り" },
  "calculator.totalReturn": { en: "Total return", ja: "総リターン" },
  "calculator.enterFdDetails": {
    en: "Enter the FD details to see results",
    ja: "結果を表示するには定期預金の詳細を入力してください",
  },
  "calculator.noCalculation": {
    en: "No calculation yet",
    ja: "まだ計算がありません",
  },
  "calculator.fillForm": {
    en: "Fill in the form to see results",
    ja: "フォームに入力すると結果が表示されます",
  },
  "calculator.loadingProducts": {
    en: "Loading products...",
    ja: "商品を読み込み中...",
  },
  "calculator.selectProduct": { en: "Select a product", ja: "商品を選択" },
  "calculator.error.calcFailed": {
    en: "Failed to calculate FD returns",
    ja: "定期預金の計算に失敗しました",
  },
  "calculator.error.productCodeInvalid": {
    en: "Selected product code is not compatible with calculator. Please choose a product with a hyphenated code (A-Z, 0-9, - only, max 20 chars).",
    ja: "選択した商品コードは計算機に対応していません。ハイフン区切りのコード（A-Z・0-9・- のみ、最大20文字）を持つ商品を選択してください。",
  },
  "calculator.error.enterProductFirst": {
    en: "Please enter a product code first",
    ja: "先に商品コードを入力してください",
  },
  "calculator.tips.title": {
    en: "FD Calculator Tips",
    ja: "定期預金計算のヒント",
  },
  "calculator.tips.higherTenure.title": {
    en: "Higher Tenure = Better Returns",
    ja: "長期期間 = 高いリターン",
  },
  "calculator.tips.higherTenure.desc": {
    en: "Longer-term FDs typically offer higher interest rates.",
    ja: "長期間の定期預金は、一般的により高い金利が適用されます。",
  },
  "calculator.tips.compounding.title": {
    en: "Compounding Frequency",
    ja: "複利頻度",
  },
  "calculator.tips.compounding.desc": {
    en: "More frequent compounding (monthly/quarterly) yields better returns.",
    ja: "複利の頻度（毎月・四半期）が高いほど、リターンが向上します。",
  },
  "calculator.tips.tax.title": { en: "Tax Implications", ja: "税制上の注意" },
  "calculator.tips.tax.desc": {
    en: "FD interest is taxable as per your income tax slab.",
    ja: "定期預金の利息は、所得税率に応じて課税されます。",
  },
  "calculator.validation.notes": { en: "Notes", ja: "注意事項" },
  "calculator.validation.tenure": {
    en: "Product tenure valid range",
    ja: "商品適用期間",
  },
  "calculator.validation.amount": { en: "Investment range", ja: "投資範囲" },
  "calculator.validation.freq": {
    en: "Compounding frequency restricted to annual for this product",
    ja: "この商品の複利頻度は年次のみです",
  },
  "calculator.validation.reason": { en: "Reason", ja: "理由" },
  "products.title": { en: "Products & Pricing", ja: "商品と価格" },
  "products.calculate": { en: "Calculate", ja: "計算" },
  "products.edit": { en: "Edit", ja: "編集" },
  "products.addProduct": { en: "Add Product", ja: "商品を追加" },
  "products.empty.title": {
    en: "No products found",
    ja: "商品が見つかりません",
  },
  "products.empty.adjustFilters": {
    en: "Try adjusting your search or filter criteria",
    ja: "検索条件やフィルターを調整してください",
  },
  "products.empty.none": {
    en: "No banking products are currently available",
    ja: "現在、利用可能な銀行商品はありません",
  },
  "products.sort.name": { en: "Name", ja: "名称" },
  "products.sort.rate": { en: "Interest Rate", ja: "金利" },
  "products.sort.amount": { en: "Min Amount", ja: "最小金額" },
  "products.sort.tenure": { en: "Tenure", ja: "期間" },
  "products.investmentRange": { en: "Investment Range", ja: "投資範囲" },
  "products.confirm.delete": {
    en: "Are you sure you want to delete this product?",
    ja: "この商品を削除してもよろしいですか？",
  },
  "products.toast.deleted": {
    en: "Product deleted successfully",
    ja: "商品を削除しました",
  },
  "products.toast.deleteFailed": {
    en: "Failed to delete product",
    ja: "商品の削除に失敗しました",
  },
  "products.toast.loadFailed": {
    en: "Failed to load products",
    ja: "商品の読み込みに失敗しました",
  },
  "dashboard.title": { en: "Dashboard", ja: "ダッシュボード" },
  "dashboard.greeting.morning": {
    en: "Good morning",
    ja: "おはようございます",
  },
  "dashboard.greeting.afternoon": { en: "Good afternoon", ja: "こんにちは" },
  "dashboard.greeting.evening": { en: "Good evening", ja: "こんばんは" },
  "dashboard.subtitle": {
    en: "Welcome to your CashCached dashboard",
    ja: "CashCachedダッシュボードへようこそ",
  },
  "dashboard.card.totalBalance": { en: "Total Balance", ja: "総残高" },
  "dashboard.card.activeAccounts": { en: "Active Accounts", ja: "有効口座" },
  "dashboard.card.availableProducts": {
    en: "Available Products",
    ja: "利用可能な商品",
  },
  "dashboard.card.recentTransactions": {
    en: "Recent Transactions",
    ja: "最近の取引",
  },
  "dashboard.quick.title": { en: "Quick Actions", ja: "クイック操作" },
  "dashboard.quick.subtitle": {
    en: "Access your most used banking features",
    ja: "よく使う機能にアクセス",
  },
  "dashboard.quick.calculator.title": {
    en: "FD Calculator",
    ja: "定期預金計算機",
  },
  "dashboard.quick.calculator.desc": {
    en: "Calculate your fixed deposit returns",
    ja: "定期預金のリターンを計算",
  },
  "dashboard.quick.accounts.title": { en: "My Accounts", ja: "マイ口座" },
  "dashboard.quick.accounts.desc": {
    en: "View your account details",
    ja: "口座詳細を表示",
  },
  "dashboard.quick.products.title": { en: "Products", ja: "商品" },
  "dashboard.quick.products.desc": {
    en: "Explore banking products",
    ja: "銀行商品の閲覧",
  },
  "dashboard.quick.profile.title": { en: "Profile", ja: "プロフィール" },
  "dashboard.quick.profile.desc": {
    en: "Manage your profile",
    ja: "プロフィール管理",
  },
  "dashboard.activity.title": {
    en: "Recent Activity",
    ja: "最近のアクティビティ",
  },
  "dashboard.activity.subtitle": {
    en: "Your latest banking activities",
    ja: "最近の銀行活動",
  },
  "dashboard.activity.accountOpened": { en: "Account opened", ja: "口座開設" },
  "dashboard.activity.accountOpened.desc": {
    en: "Fixed Deposit account created",
    ja: "定期預金口座を作成しました",
  },
  "dashboard.activity.calculatorUsed": {
    en: "FD Calculator used",
    ja: "計算機を使用",
  },
  "dashboard.activity.calculatorUsed.desc": {
    en: `Calculated returns for 100,000 ${TOKEN_SYMBOL}`,
    ja: `100,000 ${TOKEN_SYMBOL}のリターンを計算しました`,
  },
  "dashboard.activity.productViewed": {
    en: "Product viewed",
    ja: "商品を閲覧",
  },
  "dashboard.activity.productViewed.desc": {
    en: "Fixed Deposit product details",
    ja: "定期預金商品の詳細",
  },
  "dashboard.activity.today": { en: "Today", ja: "今日" },
  "dashboard.activity.yesterday": { en: "Yesterday", ja: "昨日" },
  "dashboard.activity.daysAgo2": { en: "2 days ago", ja: "2日前" },
  "dashboard.ai.title": { en: "AI Assistant", ja: "AIアシスタント" },
  "dashboard.ai.description": {
    en: "Ask questions about your accounts and products",
    ja: "口座や商品について質問する",
  },
  "dashboard.ai.placeholder": {
    en: "Ask anything...",
    ja: "何でも聞いてください...",
  },
  "dashboard.ai.send": { en: "Send", ja: "送信" },
  "dashboard.ai.thinking": { en: "Thinking...", ja: "考え中..." },
  "dashboard.ai.welcome": {
    en: "Start the conversation to receive tailored answers based on your BankTrust data.",
    ja: "会話を開始して、BankTrustデータに基づいたカスタマイズされた回答を受け取ります。",
  },
  "common.search": { en: "Search...", ja: "検索..." },
  "common.hide": { en: "Hide", ja: "非表示" },
  "common.show": { en: "Show", ja: "表示" },
  "common.category": { en: "Category", ja: "カテゴリ" },
  "common.allCategories": { en: "All Categories", ja: "すべてのカテゴリ" },
  "common.sortBy": { en: "Sort by", ja: "並び替え" },
  "common.year": { en: "year", ja: "年" },
  "common.years": { en: "years", ja: "年" },
  "common.months": { en: "months", ja: "ヶ月" },
  "common.to": { en: "to", ja: "〜" },
  "calculator.interestRate": { en: "Interest Rate", ja: "金利" },
  "auth.login.title": { en: "Sign in", ja: "ログイン" },
  "auth.login.username": { en: "Username", ja: "ユーザー名" },
  "auth.login.password": { en: "Password", ja: "パスワード" },
  "auth.login.submit": { en: "Log in", ja: "ログイン" },
  "auth.login.welcome": { en: "Welcome back", ja: "お帰りなさい" },
  "auth.login.subtitle": {
    en: "Sign in to your CashCached account to continue",
    ja: "CashCachedアカウントにログインしてください",
  },
  "auth.login.signingIn": { en: "Signing in...", ja: "ログイン中..." },
  "auth.login.noAccount": {
    en: "Don't have an account?",
    ja: "アカウントをお持ちでない場合",
  },
  "auth.login.signUp": { en: "Sign up", ja: "新規登録" },
  "auth.register.title": { en: "Create account", ja: "アカウント作成" },
  "auth.register.submit": { en: "Register", ja: "登録" },
  "auth.register.subtitle": {
    en: "Sign up for a new CashCached account",
    ja: "新しいCashCachedアカウントを作成",
  },
  "auth.register.creating": {
    en: "Creating account...",
    ja: "アカウント作成中...",
  },
  "auth.register.haveAccount": {
    en: "Already have an account?",
    ja: "すでにアカウントをお持ちですか？",
  },
  "auth.register.signIn": { en: "Sign in", ja: "ログイン" },
  "auth.field.firstName": { en: "First Name", ja: "名" },
  "auth.field.lastName": { en: "Last Name", ja: "姓" },
  "auth.field.email": { en: "Email", ja: "メール" },
  "auth.field.username": { en: "Username", ja: "ユーザー名" },
  "auth.field.phone": { en: "Phone Number", ja: "電話番号" },
  "auth.field.role": { en: "Role", ja: "権限" },
  "auth.field.password": { en: "Password", ja: "パスワード" },
  "auth.field.confirmPassword": {
    en: "Confirm Password",
    ja: "パスワード確認",
  },
  "auth.field.address": { en: "Address", ja: "住所" },
  "auth.field.dateOfBirth": { en: "Date of Birth", ja: "生年月日" },
  "auth.field.aadhaarNumber": { en: "Aadhaar Number", ja: "アーダール番号" },
  "auth.field.panNumber": { en: "PAN Number", ja: "PAN番号" },
  "auth.field.preferredCurrency": {
    en: "Preferred Currency",
    ja: "希望通貨",
  },
  "auth.placeholder.address": {
    en: "Enter your residential address",
    ja: "住所を入力してください",
  },
  "auth.placeholder.aadhaarNumber": {
    en: "Enter 12-digit Aadhaar number",
    ja: "12桁のアーダール番号を入力",
  },
  "auth.placeholder.panNumber": {
    en: "Enter PAN number (e.g., ABCDE1234F)",
    ja: "PAN番号を入力してください",
  },
  "auth.register.success": {
    en: "Registration successful!",
    ja: "登録が完了しました",
  },
  "auth.register.failed": {
    en: "Registration failed. Please try again.",
    ja: "登録に失敗しました。もう一度お試しください。",
  },
  "auth.register.password": { en: "Password", ja: "パスワード" },
  "auth.placeholder.username": {
    en: "Enter your username",
    ja: "ユーザー名を入力",
  },
  "auth.placeholder.password": {
    en: "Enter your password",
    ja: "パスワードを入力",
  },
  "auth.placeholder.confirmPassword": {
    en: "Confirm your password",
    ja: "パスワードを再入力",
  },
  "auth.placeholder.email": {
    en: "Enter your email address",
    ja: "メールアドレスを入力",
  },
  "auth.placeholder.phone": {
    en: "Enter your phone number",
    ja: "電話番号を入力",
  },
  "auth.placeholder.firstName": { en: "Enter your first name", ja: "名を入力" },
  "auth.placeholder.lastName": { en: "Enter your last name", ja: "姓を入力" },
  "auth.placeholder.role": { en: "Select a role", ja: "権限を選択" },
  "auth.otp.title": { en: "Verify OTP", ja: "OTP認証" },
  "auth.otp.subtitle": {
    en: "Enter the 6-digit OTP sent to",
    ja: "送信された6桁のOTPを入力してください",
  },
  "auth.otp.code": { en: "OTP Code", ja: "OTPコード" },
  "auth.otp.placeholder": { en: "Enter 6-digit code", ja: "6桁のコードを入力" },
  "auth.otp.verify": { en: "Verify OTP", ja: "OTP認証" },
  "auth.otp.verifying": { en: "Verifying...", ja: "認証中..." },
  "auth.otp.backToLogin": { en: "Back to login", ja: "ログインに戻る" },
  "auth.otp.sent": { en: "OTP sent", ja: "OTP送信完了" },
  "auth.otp.sentDesc": {
    en: "Check your email for the 6-digit code",
    ja: "メールで6桁のコードを確認してください",
  },
  "auth.showPassword": { en: "Show password", ja: "パスワードを表示" },
  "auth.hidePassword": { en: "Hide password", ja: "パスワードを非表示" },
  "auth.tabs.password": { en: "Password", ja: "パスワード" },
  "auth.tabs.magicLink": { en: "Magic Link", ja: "マジックリンク" },
  "auth.magicLink.title": {
    en: "Sign in with Magic Link",
    ja: "マジックリンクでサインイン",
  },
  "auth.magicLink.subtitle": {
    en: "We'll send you a secure link to sign in",
    ja: "サインインするための安全なリンクを送信します",
  },
  "auth.magicLink.emailLabel": { en: "Email address", ja: "メールアドレス" },
  "auth.magicLink.sendLink": {
    en: "Send Magic Link",
    ja: "マジックリンク送信",
  },
  "auth.magicLink.sending": {
    en: "Sending link...",
    ja: "リンクを送信中...",
  },
  "auth.magicLink.sent": {
    en: "Check your email for the magic link",
    ja: "メールでマジックリンクを確認してください",
  },
  "auth.magicLink.verifying": {
    en: "Verifying Magic Link",
    ja: "マジックリンク検証中",
  },
  "auth.magicLink.verifyingMsg": {
    en: "Please wait while we verify your link...",
    ja: "リンクを検証中です。お待ちください...",
  },
  "auth.magicLink.verifyingMsg2": {
    en: "Verifying magic link...",
    ja: "マジックリンクを検証中...",
  },
  "auth.magicLink.noToken": {
    en: "No magic link token provided",
    ja: "マジックリンクトークンが提供されていません",
  },
  "auth.magicLink.invalidOrExpired": {
    en: "Invalid or expired magic link",
    ja: "マジックリンクが無効または期限切れです",
  },
  "profile.title": { en: "My Profile", ja: "マイプロフィール" },
  "profile.update": { en: "Update Profile", ja: "プロフィール更新" },
  "profile.subtitle": {
    en: "Manage your personal information and account settings",
    ja: "個人情報とアカウント設定を管理",
  },
  "profile.edit": { en: "Edit Profile", ja: "プロフィールを編集" },
  "profile.cancel": { en: "Cancel", ja: "キャンセル" },
  "profile.saving": { en: "Saving...", ja: "保存中..." },
  "profile.saveChanges": { en: "Save Changes", ja: "変更を保存" },
  "profile.tabs.personal": { en: "Personal Information", ja: "個人情報" },
  "profile.tabs.security": { en: "Security", ja: "セキュリティ" },
  "profile.section.personal.title": {
    en: "Personal Information",
    ja: "個人情報",
  },
  "profile.section.personal.desc": {
    en: "Update your personal details and contact information",
    ja: "個人情報と連絡先を更新",
  },
  "profile.field.firstName": { en: "First Name", ja: "名" },
  "profile.field.lastName": { en: "Last Name", ja: "姓" },
  "profile.field.fullName": { en: "Full Name", ja: "フルネーム" },
  "profile.field.email": { en: "Email Address", ja: "メールアドレス" },
  "profile.field.phone": { en: "Phone Number", ja: "電話番号" },
  "profile.field.address": { en: "Address", ja: "住所" },
  "profile.field.dob": { en: "Date of Birth", ja: "生年月日" },
  "profile.field.preferredCurrency": {
    en: "Preferred Currency",
    ja: "希望通貨",
  },
  "profile.placeholder.firstName": {
    en: "Enter your first name",
    ja: "名を入力",
  },
  "profile.placeholder.lastName": {
    en: "Enter your last name",
    ja: "姓を入力",
  },
  "profile.placeholder.email": {
    en: "Enter your email address",
    ja: "メールアドレスを入力",
  },
  "profile.placeholder.phone": {
    en: "Enter your phone number",
    ja: "電話番号を入力",
  },
  "profile.placeholder.address": { en: "Enter your address", ja: "住所を入力" },
  "profile.placeholder.selectCurrency": {
    en: "Select currency",
    ja: "通貨を選択",
  },
  "profile.section.security.title": {
    en: "Security Settings",
    ja: "セキュリティ設定",
  },
  "profile.section.security.desc": {
    en: "Manage your account security and privacy settings",
    ja: "アカウントのセキュリティとプライバシーを管理",
  },
  "profile.security.2fa.title": {
    en: "Two-Factor Authentication",
    ja: "2段階認証",
  },
  "profile.security.2fa.desc": {
    en: "Add an extra layer of security to your account",
    ja: "アカウントにセキュリティを追加",
  },
  "profile.security.2fa.enable": { en: "Enable", ja: "有効化" },
  "profile.security.2fa.disable": { en: "Disable", ja: "無効化" },
  "profile.security.changePassword.title": {
    en: "Change Password",
    ja: "パスワード変更",
  },
  "profile.security.changePassword.desc": {
    en: "Update your account password",
    ja: "アカウントのパスワードを更新",
  },
  "profile.security.changePassword.change": { en: "Change", ja: "変更" },
  "profile.security.loginActivity.title": {
    en: "Login Activity",
    ja: "ログイン履歴",
  },
  "profile.security.loginActivity.desc": {
    en: "View your recent login history",
    ja: "最近のログイン履歴を表示",
  },
  "profile.security.loginActivity.view": { en: "View", ja: "表示" },
  "profile.security.loginActivity.recentLogins": {
    en: "Recent logins",
    ja: "最近のログイン",
  },
  "profile.security.loginActivity.loading": {
    en: "Loading activity…",
    ja: "アクティビティ読み込み中…",
  },
  "profile.security.loginActivity.noActivity": {
    en: "No recent activity",
    ja: "最近のアクティビティはありません",
  },
  "profile.status": { en: "Account Status:", ja: "アカウントの状態:" },
  "profile.status.active": { en: "Active", ja: "有効" },
  "profile.memberSince": { en: "Member since:", ja: "登録日:" },
  "profile.lastLogin": { en: "Last login:", ja: "最終ログイン:" },
  "app.name": { en: "CashCached", ja: "CashCached" },
  "app.description": {
    en: "Digital Banking Platform",
    ja: "デジタルバンキングプラットフォーム",
  },
  "app.accounts": { en: "CashCached accounts", ja: "CashCached口座" },
  "landing.nav.signIn": { en: "Sign In", ja: "ログイン" },
  "landing.nav.getStarted": { en: "Get Started", ja: "始める" },
  "landing.nav.dashboard": { en: "Go to Dashboard", ja: "ダッシュボードへ" },
  "landing.hero.title": {
    en: "Secure Banking on the Blockchain",
    ja: "ブロックチェーン上のセキュアバンキング",
  },
  "landing.hero.subtitle": {
    en: "Experience the future of fixed deposits with blockchain-backed security and AI-powered assistance. CashCached brings transparency, security, and intelligence to your banking.",
    ja: "ブロックチェーンバックアップされたセキュリティとAIアシスタンスで、定期預金の未来を体験してください。CashCachedは透明性、セキュリティ、インテリジェンスをあなたの銀行業務にもたらします。",
  },
  "landing.hero.openAccount": { en: "Open Account", ja: "口座を開く" },
  "landing.hero.learnMore": { en: "Learn More", ja: "詳細を見る" },
  "landing.hero.stat1.label": {
    en: "Blockchain Secured",
    ja: "ブロックチェーン保護",
  },
  "landing.hero.stat2.label": { en: "AI Assistant", ja: "AIアシスタント" },
  "landing.hero.stat3.label": { en: "Hidden Fees", ja: "隠れた手数料" },
  "landing.hero.currentRate": { en: "Current Rate", ja: "現在の金利" },
  "landing.hero.investmentOverview": {
    en: "Investment Overview",
    ja: "投資概要",
  },
  "landing.hero.principal": { en: "Principal", ja: "元本" },
  "landing.hero.maturity": { en: "Maturity", ja: "満期金額" },
  "landing.features.title": {
    en: "Why Choose CashCached?",
    ja: "CashCachedを選ぶ理由は？",
  },
  "landing.features.subtitle": {
    en: "A complete banking solution with cutting-edge technology and user-centric design",
    ja: "最先端技術とユーザー中心設計による完全な銀行ソリューション",
  },
  "landing.features.security": {
    en: "Blockchain Security",
    ja: "ブロックチェーンセキュリティ",
  },
  "landing.features.security.desc": {
    en: "Your accounts are secured by blockchain technology, ensuring immutability and transparency.",
    ja: "あなたの口座はブロックチェーン技術で保護され、不変性と透明性を保証します。",
  },
  "landing.features.ai": { en: "AI Assistant", ja: "AIアシスタント" },
  "landing.features.ai.desc": {
    en: "Get instant answers to banking queries with our intelligent chatbot available 24/7.",
    ja: "24/7利用可能なインテリジェントチャットボットで銀行業務の質問に即座に答えます。",
  },
  "landing.features.calculation": {
    en: "Smart Calculations",
    ja: "スマート計算",
  },
  "landing.features.calculation.desc": {
    en: "Automated FD calculations with multiple compounding frequencies for optimal returns.",
    ja: "最適なリターンのための複数の複利頻度による自動FD計算。",
  },
  "landing.features.instant": { en: "Instant Processing", ja: "即時処理" },
  "landing.features.instant.desc": {
    en: "Create accounts, calculate maturity, and manage investments in seconds.",
    ja: "口座を開設し、満期を計算し、投資を数秒で管理します。",
  },
  "landing.features.security2": {
    en: "Bank-Grade Security",
    ja: "銀行グレードセキュリティ",
  },
  "landing.features.security2.desc": {
    en: "JWT authentication, role-based access, and encrypted data storage.",
    ja: "JWT認証、ロールベースアクセス、暗号化されたデータストレージ。",
  },
  "landing.features.transparent": {
    en: "Transparent Pricing",
    ja: "透明な価格設定",
  },
  "landing.features.transparent.desc": {
    en: "No hidden fees. See exactly what you earn with complete pricing transparency.",
    ja: "隠れた手数料なし。完全な価格透明性で正確に得られるものを確認してください。",
  },
  "landing.steps.title": {
    en: "Get Started in 3 Simple Steps",
    ja: "3つの簡単なステップで始める",
  },
  "landing.steps.subtitle": {
    en: "Start your journey to secure banking today",
    ja: "今日からセキュアバンキングの旅を始めましょう",
  },
  "landing.steps.1.title": { en: "Create Account", ja: "アカウント作成" },
  "landing.steps.1.desc": {
    en: "Sign up with your email and verify your identity. Takes less than 5 minutes.",
    ja: "メールで登録して身元を確認します。5分以内に完了します。",
  },
  "landing.steps.2.title": { en: "Choose Product", ja: "商品を選択" },
  "landing.steps.2.desc": {
    en: "Browse our FD products with competitive rates and flexible terms.",
    ja: "競争力のある金利と柔軟な条件でFD商品を閲覧します。",
  },
  "landing.steps.3.title": { en: "Invest & Earn", ja: "投資して稼ぐ" },
  "landing.steps.3.desc": {
    en: "Start your FD and watch your money grow with transparent tracking.",
    ja: "FDを開始し、透明な追跡でお金の増加を見守ります。",
  },
  "landing.stats.users": { en: "Active Users", ja: "アクティブユーザー" },
  "landing.stats.aum": { en: "Assets Under Management", ja: "運用資産" },
  "landing.stats.uptime": {
    en: "Platform Uptime",
    ja: "プラットフォーム稼働率",
  },
  "landing.stats.rating": { en: "User Rating", ja: "ユーザー評価" },
  "landing.cta.title.auth": {
    en: "Welcome Back to CashCached",
    ja: "CashCachedにお帰りなさい",
  },
  "landing.cta.title.guest": {
    en: "Ready to Transform Your Banking?",
    ja: "銀行業務を変革する準備はできていますか？",
  },
  "landing.cta.subtitle.auth": {
    en: "Access your dashboard to manage your investments and accounts.",
    ja: "ダッシュボードにアクセスして投資と口座を管理します。",
  },
  "landing.cta.subtitle.guest": {
    en: "Join thousands of users who trust CashCached for secure, transparent, and intelligent banking.",
    ja: "セキュア、透明、インテリジェントなバンキングのためにCashCachedを信頼する数千のユーザーに参加してください。",
  },
  "landing.cta.start": { en: "Start Free Today", ja: "今日から無料で始める" },
  "landing.cta.demo": { en: "Schedule Demo", ja: "デモをスケジュール" },
  "landing.footer.tagline": {
    en: "Blockchain-backed banking for the modern era.",
    ja: "モダンエラのためのブロックチェーンバックアップバンキング。",
  },
  "landing.footer.product": { en: "Product", ja: "商品" },
  "landing.footer.company": { en: "Company", ja: "会社" },
  "landing.footer.legal": { en: "Legal", ja: "法務" },
  "landing.footer.features": { en: "Features", ja: "機能" },
  "landing.footer.pricing": { en: "Pricing", ja: "価格設定" },
  "landing.footer.security": { en: "Security", ja: "セキュリティ" },
  "landing.footer.about": { en: "About", ja: "について" },
  "landing.footer.blog": { en: "Blog", ja: "ブログ" },
  "landing.footer.careers": { en: "Careers", ja: "キャリア" },
  "landing.footer.privacy": { en: "Privacy", ja: "プライバシー" },
  "landing.footer.terms": { en: "Terms", ja: "利用規約" },
  "landing.footer.contact": { en: "Contact", ja: "お問い合わせ" },
  "landing.footer.copyright": {
    en: "© 2024 CashCached. All rights reserved. | Blockchain-powered banking platform",
    ja: "© 2024 CashCached。著作権所有。| ブロックチェーン搭載バンキングプラットフォーム",
  },
  "sidebar.wallet.title": { en: "Wallet Balance", ja: "ウォレット残高" },
  "sidebar.wallet.depositWithdraw": {
    en: "Deposit/Withdraw",
    ja: "入金/出金",
  },
  "sidebar.wallet.history": { en: "History", ja: "履歴" },
  "sidebar.wallet.loading": { en: "Loading…", ja: "読み込み中…" },
  "role.CUSTOMER": { en: "Customer", ja: "顧客" },
  "role.ADMIN": { en: "Admin", ja: "管理者" },
  "role.BANKOFFICER": { en: "Bank Officer", ja: "銀行員" },
  "role.STAFF": { en: "Staff", ja: "スタッフ" },
  "accounts.create.title": {
    en: "Create FD Account",
    ja: "定期預金口座を開設",
  },
  "accounts.create.button": { en: "Create Account", ja: "口座を開設" },
  "accounts.create.openAccount": { en: "Open Account", ja: "口座開設" },
  "accounts.create.selectCustomer": { en: "Select Customer", ja: "顧客を選択" },
  "accounts.create.selectCustomerDescription": {
    en: "Choose the customer for whom you want to open an account",
    ja: "口座を開設する顧客を選択してください",
  },
  "accounts.create.customer": { en: "Customer", ja: "顧客" },
  "accounts.create.selectCustomerPlaceholder": {
    en: "Select a customer",
    ja: "顧客を選択",
  },
  "accounts.create.loadingCustomers": {
    en: "Loading customers...",
    ja: "顧客を読み込み中...",
  },
  "accounts.create.noCustomers": {
    en: "No customers available",
    ja: "利用可能な顧客がいません",
  },
  "accounts.create.description": {
    en: "Create a new Fixed Deposit account with standard or custom parameters",
    ja: "標準またはカスタムパラメータで新しい定期預金口座を作成",
  },
  "accounts.create.standardTab": {
    en: "Standard Account",
    ja: "標準口座",
  },
  "accounts.create.customTab": { en: "Custom Account", ja: "カスタム口座" },
  "accounts.create.productDefaults": {
    en: "Product Defaults",
    ja: "商品デフォルト",
  },
  "accounts.create.flexible": { en: "Flexible", ja: "柔軟" },
  "accounts.create.standardTitle": {
    en: "Standard FD Account",
    ja: "標準定期預金口座",
  },
  "accounts.create.standardDescription": {
    en: "Create account with default interest rate and tenure from the selected product. Both standard account number and IBAN will be generated automatically.",
    ja: "選択した商品のデフォルトの金利と期間で口座を作成します。標準口座番号とIBANが自動的に生成されます。",
  },
  "accounts.create.customTitle": {
    en: "Custom FD Account",
    ja: "カスタム定期預金口座",
  },
  "accounts.create.customDescription": {
    en: "Create account with custom interest rate and tenure within product limits. Both standard account number and IBAN will be generated automatically.",
    ja: "商品制限内でカスタム金利と期間で口座を作成します。標準口座番号とIBANが自動的に生成されます。",
  },
  "accounts.create.customerId": { en: "Customer ID", ja: "顧客ID" },
  "accounts.create.customerIdPlaceholder": {
    en: "Enter customer ID",
    ja: "顧客IDを入力",
  },
  "accounts.create.product": { en: "Product", ja: "商品" },
  "accounts.create.selectProduct": {
    en: "Select a product",
    ja: "商品を選択",
  },
  "accounts.create.loadingProducts": {
    en: "Loading products...",
    ja: "商品を読み込み中...",
  },
  "accounts.create.productDefaultsLabel": {
    en: "Product Defaults:",
    ja: "商品デフォルト：",
  },
  "accounts.create.productLimitsLabel": {
    en: "Product Limits:",
    ja: "商品制限：",
  },
  "accounts.create.interestRate": { en: "Interest Rate", ja: "金利" },
  "accounts.create.tenure": { en: "Tenure", ja: "期間" },
  "accounts.create.range": { en: "Range", ja: "範囲" },
  "accounts.create.amount": { en: "Amount", ja: "金額" },
  "accounts.create.principalAmount": {
    en: "Principal Amount (CashCached Tokens)",
    ja: "元本金額（CashCachedトークン）",
  },
  "accounts.create.principalAmountPlaceholder": {
    en: "Enter principal amount",
    ja: "元本金額を入力",
  },
  "accounts.create.customInterestRate": {
    en: "Custom Interest Rate (%)",
    ja: "カスタム金利（％）",
  },
  "accounts.create.customTenure": {
    en: "Custom Tenure (Months)",
    ja: "カスタム期間（月）",
  },
  "accounts.create.branchCode": { en: "Branch Code", ja: "支店コード" },
  "accounts.create.branchCodeHint": {
    en: "Alphanumeric uppercase (3-20 characters)",
    ja: "英数字大文字（3-20文字）",
  },
  "accounts.create.remarks": { en: "Remarks (Optional)", ja: "備考（任意）" },
  "accounts.create.remarksPlaceholder": {
    en: "Additional notes or remarks",
    ja: "追加のメモまたは備考",
  },
  "accounts.create.creating": {
    en: "Creating Account...",
    ja: "口座を作成中...",
  },
  "accounts.create.createStandard": {
    en: "Create Standard Account",
    ja: "標準口座を作成",
  },
  "accounts.create.createCustom": {
    en: "Create Custom Account",
    ja: "カスタム口座を作成",
  },
  "accounts.create.successDefault": {
    en: "Account created successfully with product defaults!",
    ja: "商品デフォルトで口座が正常に作成されました！",
  },
  "accounts.create.successCustom": {
    en: "Account created successfully with custom values!",
    ja: "カスタム値で口座が正常に作成されました！",
  },
  "accounts.create.error": {
    en: "Failed to create account",
    ja: "口座の作成に失敗しました",
  },
  "admin.accounts.title": { en: "All Accounts", ja: "すべての口座" },
  "admin.accounts.description": {
    en: "Manage and view all customer accounts with advanced filters and pagination",
    ja: "高度なフィルターとページネーションですべての顧客口座を管理・表示",
  },
  "admin.accounts.filters": { en: "Filters", ja: "フィルター" },
  "admin.accounts.filtersDescription": {
    en: "Search and filter accounts by various criteria",
    ja: "さまざまな条件で口座を検索・フィルタリング",
  },
  "admin.accounts.customerId": { en: "Customer ID", ja: "顧客ID" },
  "admin.accounts.customerIdPlaceholder": {
    en: "Enter customer ID",
    ja: "顧客IDを入力",
  },
  "admin.accounts.productCode": { en: "Product Code", ja: "商品コード" },
  "admin.accounts.productCodePlaceholder": {
    en: "Enter product code",
    ja: "商品コードを入力",
  },
  "admin.accounts.status": { en: "Status", ja: "ステータス" },
  "admin.accounts.statusPlaceholder": {
    en: "Select status",
    ja: "ステータスを選択",
  },
  "admin.accounts.allStatus": { en: "All Status", ja: "すべてのステータス" },
  "admin.accounts.active": { en: "Active", ja: "アクティブ" },
  "admin.accounts.closed": { en: "Closed", ja: "閉鎖" },
  "admin.accounts.matured": { en: "Matured", ja: "満期" },
  "admin.accounts.suspended": { en: "Suspended", ja: "停止中" },
  "admin.accounts.branchCode": { en: "Branch Code", ja: "支店コード" },
  "admin.accounts.branchCodePlaceholder": {
    en: "Enter branch code",
    ja: "支店コードを入力",
  },
  "admin.accounts.list": { en: "Accounts List", ja: "口座リスト" },
  "admin.accounts.showing": {
    en: "Showing {{from}} to {{to}} of {{total}} accounts",
    ja: "{{total}}件中{{from}}〜{{to}}件を表示",
  },
  "admin.accounts.pageSize": { en: "Per page", ja: "件数" },
  "admin.accounts.accountNo": { en: "Account No", ja: "口座番号" },
  "admin.accounts.principal": { en: "Principal", ja: "元本" },
  "admin.accounts.interestRate": { en: "Interest Rate", ja: "金利" },
  "admin.accounts.tenure": { en: "Tenure", ja: "期間" },
  "admin.accounts.actions": { en: "Actions", ja: "操作" },
  "admin.accounts.noAccounts": {
    en: "No accounts found",
    ja: "口座が見つかりません",
  },
  "admin.accounts.page": {
    en: "Page {{current}} of {{total}}",
    ja: "{{total}}ページ中{{current}}ページ",
  },
  "action.search": { en: "Search", ja: "検索" },
  "action.reset": { en: "Reset", ja: "リセット" },
  "action.previous": { en: "Previous", ja: "前へ" },
  "action.next": { en: "Next", ja: "次へ" },
  "action.details": { en: "Details", ja: "詳細" },
  "nav.administration": { en: "Administration", ja: "管理" },
  "common.loading": { en: "Loading...", ja: "読み込み中..." },
  "common.min": { en: "Min", ja: "最小" },
  "common.max": { en: "Max", ja: "最大" },
  "common.tokens": { en: "tokens", ja: "トークン" },
  "common.optional": { en: "Optional", ja: "任意" },
  "common.range": { en: "Range", ja: "範囲" },
  "common.cancel": { en: "Cancel", ja: "キャンセル" },
};

interface I18nContextType {
  lang: Lang;
  setLang: (l: Lang) => void;
  t: (key: string) => string;
}

const I18nContext = createContext<I18nContextType | undefined>(undefined);

export function I18nProvider({ children }: { children: ReactNode }) {
  const [lang, setLang] = useState<Lang>(() => {
    if (typeof window === "undefined") return "en";
    const saved = localStorage.getItem("i18n-lang");
    return (saved as Lang) || "en";
  });

  useEffect(() => {
    localStorage.setItem("i18n-lang", lang);
    document.documentElement.lang = lang;
  }, [lang]);

  const missing = useMemo(() => new Set<string>(), []);

  const t = (key: string) => {
    const item = dict[key];
    if (!item) {
      if (!missing.has(`${key}.missing`)) {
        missing.add(`${key}.missing`);
        console.warn(`[i18n] Missing key: ${key}`);
      }
      return key;
    }
    const value = item[lang] ?? item.en;
    if (item[lang] == null && !missing.has(`${key}.${lang}`)) {
      missing.add(`${key}.${lang}`);
      console.warn(`[i18n] Missing locale for key: ${key}.${lang}`);
    }
    return value;
  };

  const value = useMemo(() => ({ lang, setLang, t }), [lang]);
  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n() {
  const ctx = useContext(I18nContext);
  if (!ctx) throw new Error("useI18n must be used within I18nProvider");
  return ctx;
}
