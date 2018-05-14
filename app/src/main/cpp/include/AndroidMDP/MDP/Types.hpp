#ifndef ANDROIDMDP_MDP_TYPES_HPP
#define ANDROIDMDP_MDP_TYPES_HPP

#include <vector>

#include <AndroidMDP/Types.hpp>

namespace AndroidMDP
{
    namespace MDP
    {
        using Values = Vector;
        using Actions = std::vector<size_t>;

        struct ValueFunction
        {
            Values values;
            Actions actions;

            ValueFunction() {}
            ValueFunction(Values v, Actions a) :
                    values(std::move(v)), actions(std::move(a)) {}
        };
        using QFunction = Matrix2D;

        template <typename M>
        struct is_generative_model
        {
        private:
            template<typename Z>
            static constexpr auto test(int) -> decltype(
                    static_cast<size_t (Z::*)() const> (&Z::getS),
                    static_cast<size_t (Z::*)() const> (&Z::getA),
                    static_cast<double (Z::*)() const> (&Z::getDiscount),
                    static_cast<std::tuple<size_t, double> (Z::*)(size_t, size_t) const> (&Z::sampleSR),
                    static_cast<bool (Z::*)(size_t) const> (&Z::isTerminal),
                    bool()
            )
            { return true; }

            template<typename>
            static constexpr auto test(...) -> bool
            { return false; }

        public:
            enum
            {
                value = test<M>(0)
            };
        };

        template <typename M>
        struct is_model
        {
        private:
            template <typename Z>
            static constexpr auto test(int) -> decltype(
                    static_cast<double (Z::*)(size_t, size_t, size_t) const> (&Z::getTransitionProbability),
                    static_cast<double (Z::*)(size_t, size_t, size_t) const> (&Z::getExpectedReward),
                    bool()
            )
            { return true; }

            template <typename>
            static constexpr auto test(...) -> bool
            { return false; }

        public:
            enum
            {
                value = test<M>(0) && is_generative_model<M>::value
            };
        };

        template<typename M>
        struct is_model_eigen
        {
        private:
            #define RETVAL_EXTRACTOR(fun_name)                                                                                                  \
                                                                                                                                                \
            template <typename Z, typename ...Args> static auto fun_name##RetType(Z* z) ->                                                      \
                                                                typename remove_cv_ref<decltype(z->fun_name(std::declval<Args>()...))>::type;   \
                                                                                                                                                \
            template <typename Z, typename ...Args> static auto fun_name##RetType(...) -> int

            RETVAL_EXTRACTOR(getTransitionFunction);
            RETVAL_EXTRACTOR(getRewardFunction);

            using F = decltype(getTransitionFunctionRetType<const M, size_t>(0));
            using R = decltype(getRewardFunctionRetType<const M>(0));

            template <typename Z> static constexpr auto test(int) -> decltype(

                static_cast<const F & (Z::*)(size_t) const>     (&Z::getTranstionFunction),
                static_cast<const R & (Z::*)()       const>     (&Z::getRewardFucntion),
                bool()
            ) { return true; }
            template<typename Z> static constexpr auto test(...) -> bool { return false; }

            #undef RETVAL_EXTRACTOR
        public:
            enum
            {
                value = is_model<M>::value && test<M> &&
                        std::is_base_of<Eigen::EigenBase<F>, F>::value &&
                        std::is_base_of<Eigen::EigenBase<R>, R>::value
            };
        };
    }
}

#endif
