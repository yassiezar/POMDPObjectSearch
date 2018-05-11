#include <AndroidMDP/MDP/SparseModel.hpp>

namespace AndroidMDP
{
    namespace MDP
    {
        template <typename M, typename>
        SparseModel::SparseModel(const M &model) :
                S(model.getS()), A(model.getA()), transitions_(A, SparseMatrix2D(S, S)),
                rewards_(S, A), rand_(Util::Seed::getSeed())
        {
            setDiscount(model.getDiscount());

            for(size_t s = 0; s < S; s ++)
            {
                for(size_t a = 0; a < A; a ++)
                {
                    for(size_t s1 = 0; s1 < S; s1 ++)
                    {
                        const double p = model.getTransitionProbability(s, a, s1);
                        if(p < 0.0 || p > 1.0) throw std::invalid_argument("Input transition table does not contain valid probabilities.\n");
                        if(Util::checkDifferentSmall(0.0, p)) transitions_[a].insert(s, s1) = p;

                        const double r = model.getExpectedReward(s, a, s1);
                        if(Util::checkDifferentSmall(0.0, r)) rewards_.coeffRef(s, a) += r * p;
                    }
                    if(Util::checkDifferentSmall(1.0, transitions_[a].row(s).sum())) throw std::invalid_argument("Input transition table does not contain valid probabilities.\n");
                }
            }
            for(size_t a = 0; a < A; a ++)
            {
                transitions_[a].makeCompressed();
            }
            rewards_.makeCompressed();
        }

        SparseModel::SparseModel(const size_t s, const size_t a, const double discount) :
                S(s), A(a), discount_(discount), transitions_(A, SparseMatrix2D(S, S)),
                rewards_(S, A), rand_(Util::Seed::getSeed())
        {
            for(size_t a = 0; a < A; a ++)
            {
                transitions_[a].setIdentity();
            }
        }

        size_t SparseModel::getA() const { return A; }
        size_t SparseModel::getS() const { return S; }
        double SparseModel::getDiscount() const { return discount_; }

        double SparseModel::getTransitionProbability(const size_t s, const size_t a, const size_t s1) const
        {
            return transitions_[a].coeff(s, s1);
        }

        double SparseModel::getExpectedReward(size_t s, size_t a, size_t s1) const
        {
            return rewards_.coeff(s, a);
        }

        bool SparseModel::isTerminal(const size_t s) const
        {
            for(size_t a = 0; a < A; a ++)
            {
                if(!Util::checkEqualSmall(1.0, getTransitionProbability(s, a, s))) return false;
            }
            return true;
        }

        std::tuple<size_t, double> SparseModel::sampleSR(const size_t s, const size_t a) const
        {
            const size_t s1 = sampleProbability(S, transitions_[a].row(s), rand_);
            return std::make_tuple(s1, getExpectedReward(s, a, s1));
        }

        void SparseModel::setTransitionFunction(const AndroidMDP::SparseMatrix3D &t)
        {
            for(size_t a = 0; a < A; a ++)
            {
                for(size_t s = 0; s < S; s ++)
                {
                    if(!Util::checkEqualSmall(1.0, t[a].row(s).sum()))
                    {
                        throw std::invalid_argument("Invalid input transition function.");
                    }
                    if(!Util::checkEqualSmall(1.0, t[a].row(s).cwiseAbs().sum()))
                    {
                        throw std::invalid_argument("Invalid input transition function.");
                    }
                }
            }
            transitions_ = t;
        }

        void SparseModel::setRewardFunction(const AndroidMDP::SparseMatrix2D &r)
        {
            rewards_ = r;
        }
    }
}